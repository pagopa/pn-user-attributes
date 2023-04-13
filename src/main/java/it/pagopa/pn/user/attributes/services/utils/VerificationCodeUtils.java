package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnExpiredVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidInputException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnRetryLimitVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationCodeUtils {

    private final AddressBookDao dao;
    private final PnUserattributesConfig pnUserattributesConfig;
    private final PnDataVaultClient dataVaultClient;
    private final PnExternalChannelClient pnExternalChannelClient;
    private final VerifiedAddressUtils verifiedAddressUtils;
    private final SecureRandom rnd = new SecureRandom();

    /**
     * Valida un codice di verifica e lo anonimizza
     *
     * @param recipientId id utente
     * @param verificationCode codice verifica
     * @param legalChannelType tipo canale legale
     * @param courtesyChannelType tipo canale
     * @return risultato dell'operazione
     */
    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> validateVerificationCodeAndSendToDataVault(String recipientId, AddressVerificationDto verificationCode,
                                                                                                   LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType) {
        String hashedaddress = hashAddress(verificationCode.getValue());
        String legal = getLegalType(legalChannelType);
        String channelType = getChannelType(legalChannelType, courtesyChannelType);

        log.info("validating code uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, hashedaddress, channelType, legal);
        return retrieveVerificationCode(recipientId, verificationCode, hashedaddress, channelType)
                .flatMap(r -> manageAttempts(r, verificationCode.getVerificationCode()))
                .doOnSuccess(r -> log.info("Verification code validated uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, hashedaddress, channelType, legal))
                .flatMap(r -> checkValidPecAndSendToDataVaultAndSaveInDynamodb(r, legalChannelType, verificationCode.getValue() != null?verificationCode.getValue():r.getPecAddress(), legal))
                .switchIfEmpty(Mono.error(new PnExpiredVerificationCodeException()));
    }

    /**
     * Recupera un indirizzo da validare, in base al value o al requestId (in questo caso viene validato il recipientId)
     *
     * @param recipientId recipientId
     * @param verificationCode oggetto con info della verifica
     * @param hashedaddress indirizzo hashato
     * @param channelType canale
     * @return l'entity se presente
     */
    private Mono<VerificationCodeEntity> retrieveVerificationCode(String recipientId, AddressVerificationDto verificationCode, String hashedaddress, String channelType) {
        if (verificationCode.getRequestId() != null) {
            return dao.getVerificationCodeByRequestId(verificationCode.getRequestId())
                    .filter(verificationCodeEntity -> verificationCodeEntity.getRecipientId().equals(recipientId));
        } else {
            VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(recipientId, hashedaddress, channelType, null);
            return dao.getVerificationCode(verificationCodeEntity);
        }
    }


    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @param verificationCodeEntity verificationCodeEntity
     * @param realaddress indirizzo da salvare
     * @param legal tipologia legal
     * @return risultato dell'operazione
     */
    private Mono<AddressBookService.SAVE_ADDRESS_RESULT> checkValidPecAndSendToDataVaultAndSaveInDynamodb(VerificationCodeEntity verificationCodeEntity, LegalChannelTypeDto legalChannelType, String realaddress, String legal)
    {
        verificationCodeEntity.setCodeValid(true);
        if (legalChannelType == LegalChannelTypeDto.PEC && !verificationCodeEntity.isPecValid()) {
            log.info("checkValidPecAndSendToDataVaultAndSaveInDynamodb set codeValid=true internalId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress());
            return this.dao.updateVerificationCodeIfExists(verificationCodeEntity)
                    .then(Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.PEC_VALIDATION_REQUIRED));
        } else {
            log.info("saving address in datavault internalId:{} hashedaddress:{} channel:{}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress(), verificationCodeEntity.getChannelType());
            return sendToDataVaultAndSaveInDynamodb(verificationCodeEntity.getRecipientId(), realaddress, legal, verificationCodeEntity.getSenderId(), verificationCodeEntity.getChannelType());
        }
    }

    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @param recipientId idutente
     * @param realaddress indirizzo da salvare
     * @param legal tipologia
     * @param senderId eventuale preferenza mittente
     * @param channelType tipologia canale
     * @return risultato dell'operazione
     */
    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> sendToDataVaultAndSaveInDynamodb(String recipientId, String realaddress, String legal, String senderId, String channelType)
    {
        String hashedaddress = hashAddress(realaddress);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);
        addressBookEntity.setAddresshash(hashedaddress);
        String addressId = addressBookEntity.getAddressId();   //l'addressId è l'SK!
        log.info("saving address in datavault uid:{} hashedaddress:{} channel:{} legal:{}", recipientId, hashedaddress, channelType, legal);
        return this.dataVaultClient.updateRecipientAddressByInternalId(recipientId, addressId, realaddress)
                .then(verifiedAddressUtils.saveInDynamodb(addressBookEntity))
                .then(Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS));
    }

    /**
     * Genera, salva e invia a ext-channel un nuovo codice di verifica
     *
     * @param recipientId id utente
     * @param realaddress indirizzo utente
     * @param legalChannelType eventuale tipo canale legale
     * @param courtesyChannelType eventuale tipo canale cortesia
     * @return risultato dell'operazione
     */
    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> saveInDynamodbNewVerificationCodeAndSendToExternalChannel(String recipientId, String realaddress,
                                                                                                                  LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String senderId, String address) {
        String hashedaddress = hashAddress(realaddress);
        String vercode = getNewVerificationCode();
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        log.info("saving new verificationcode and send it to ext channel uid:{} hashedaddress:{} channel:{} newvercode:{}", recipientId, hashedaddress, channelType, vercode);
        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashedaddress, channelType, senderId);
        verificationCode.setVerificationCode(vercode);
        if (legalChannelType == LegalChannelTypeDto.PEC)
            verificationCode.setPecAddress(address);
        verificationCode.setTtl(LocalDateTime.now().plus(pnUserattributesConfig.getVerificationCodeTTL()).atZone(ZoneId.systemDefault()).toEpochSecond());

        return dao.saveVerificationCode(verificationCode)
                .zipWhen(r -> pnExternalChannelClient.sendVerificationCode(recipientId, realaddress, legalChannelType, courtesyChannelType, verificationCode.getVerificationCode())
                                .flatMap(requestId -> {
                                    // aggiorno il requestId
                                    verificationCode.setRequestId(requestId);
                                    return dao.updateVerificationCodeIfExists(verificationCode);
                                })
                        ,(r, v) -> AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
    }

    private Mono<VerificationCodeEntity> manageAttempts(VerificationCodeEntity verificationCodeEntity, String verificationCode) {
        if (verificationCodeEntity.getLastModified().isBefore(Instant.now().minus(pnUserattributesConfig.getVerificationCodeTTL()))) {
            // trovato scaduto, elimino
            return dao.deleteVerificationCode(verificationCodeEntity)
                    .then(Mono.error(new PnExpiredVerificationCodeException()));
        }
        else if (verificationCodeEntity.getVerificationCode().equals(verificationCode)) {
            return Mono.just(verificationCodeEntity);
        }
        else {
            // codice errato, incremento i tentativi (o elimino se ho raggiungo il massimo)
            verificationCodeEntity.setFailedAttempts(verificationCodeEntity.getFailedAttempts()+1);
            if (verificationCodeEntity.getFailedAttempts() >= pnUserattributesConfig.getValidationCodeMaxAttempts()) {
                // raggiunto limite tentativi, elimino
                return dao.deleteVerificationCode(verificationCodeEntity)
                        .then(Mono.error(new PnRetryLimitVerificationCodeException()));
            } else {
                // codice errato ma ci sono ancora tentativi disponibili
                return dao.updateVerificationCodeIfExists(verificationCodeEntity)
                        .then(Mono.error(new PnInvalidVerificationCodeException()));
            }
        }
    }




    public void validateAddress(LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto) {
        // se è specificato il requestId, non mi interessa il value
        if (addressVerificationDto.getRequestId() != null)
        {
            return;
        }

        // aggiungo il controllo dato che ora value è nullabile
        String emailfield = "value";
        if (!StringUtils.hasText(addressVerificationDto.getValue()))
            throw new PnInvalidInputException(PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, emailfield);

        if ((legalChannelType != null && legalChannelType.equals(LegalChannelTypeDto.PEC))
                || (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.EMAIL)))
        {
            String emailaddress = addressVerificationDto.getValue();

            final Pattern emailRegex = Pattern.compile("^[\\p{L}0-9!#\\$%*/?\\|\\^\\{\\}`~&'+\\-=_]+(?:[.-][\\p{L}0-9!#\\$%*/?\\|\\^\\{\\}`~&'+\\-=_]+){0,10}@\\w+(?:[.-]\\w+){0,10}\\.\\w{2,10}$", Pattern.CASE_INSENSITIVE);
            if (!emailRegex.matcher(emailaddress).matches())
                throw new PnInvalidInputException(PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, emailfield);
        }
        else if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.SMS))
        {
            final Pattern phoneRegex = Pattern.compile("^(00|\\+)393\\d{8,9}$", Pattern.CASE_INSENSITIVE);
            if (!phoneRegex.matcher(addressVerificationDto.getValue()).matches())
                throw new PnInvalidInputException(PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, emailfield);
        }
    }


    /**
     * Genera un nuovo codice
     *
     * @return il codice generato
     */
    private String getNewVerificationCode() {
        // It will generate 5 digit random Number.
        // from 0 to 99999
        int number = rnd.nextInt(99999);

        // this will convert any number sequence into 5 character.
        String code = String.format("%05d", number);
        log.debug("generated a new verificationCode: {}", code);
        return code;
    }

    /**
     * Wrap dello sha per rendere più facile capire dove viene usato
     * @param realaddress indirizzo da hashare
     * @return hash dell'indirizzo
     */
    public String hashAddress(@NonNull String realaddress)
    {
        return DigestUtils.sha256Hex(realaddress);
    }


    /**
     * Ricava il tipo di channel in base agli enum.
     * Ci si aspetta che solo un parametro sia valorizzato per volta
     *
     * @param legalChannelType eventuale channelType legale
     * @param courtesyChannelType eventuale channelType cortesia
     * @return stringa rappresentante il channelType
     */
    public String getChannelType(LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType)
    {
        return legalChannelType!=null?legalChannelType.getValue():courtesyChannelType.getValue();
    }

    /**
     * Ricava il tipo di canale (legale o cortesia)
     *
     * @param legalChannelType eventuale channelType legale. Se null, si intende di cortesia
     * @return stringa rappresentante il tipo di canale
     */
    public String getLegalType(LegalChannelTypeDto legalChannelType)
    {
        return legalChannelType!=null? LegalAddressTypeDto.LEGAL.getValue(): CourtesyAddressTypeDto.COURTESY.getValue();
    }
}
