package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.*;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;

import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;

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
        String legal = getLegalType(legalChannelType);
        String channelType = getChannelType(legalChannelType, courtesyChannelType);

        log.info("validating code recipientId:{} requestId:{} channel:{} addrtype:{}", recipientId, verificationCode.getRequestId(), channelType, legal);
        return retrieveVerificationCode(recipientId, verificationCode, channelType)
                .switchIfEmpty(Mono.error(new PnExpiredVerificationCodeException()))
                .flatMap(r -> manageAttempts(r, verificationCode.getVerificationCode(), legalChannelType))
                .doOnSuccess(r -> log.info("Verification code validated uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, r==null?"NULL":r.getHashedAddress(), channelType, legal))
                .flatMap(r -> checkValidPecAndSendToDataVaultAndSaveInDynamodb(r, legalChannelType))
                .switchIfEmpty(Mono.error(new PnExpiredVerificationCodeException()));
    }

    /**
     * Recupera un indirizzo da validare, in base al value o al requestId (in questo caso viene validato il recipientId)
     *
     * @param recipientId recipientId
     * @param verificationCode oggetto con info della verifica
     * @param channelType canale
     * @return l'entity se presente
     */
    private Mono<VerificationCodeEntity> retrieveVerificationCode(String recipientId, AddressVerificationDto verificationCode, String channelType) {
        if (verificationCode.getRequestId() != null) {
            return dao.getVerificationCodeByRequestId(verificationCode.getRequestId())
                    .filter(verificationCodeEntity -> verificationCodeEntity.getRecipientId().equals(recipientId));
        } else {
            String hashedaddress = hashAddress(verificationCode.getValue());
            VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(recipientId, hashedaddress, channelType);
            return dao.getVerificationCode(verificationCodeEntity);
        }
    }


    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @param verificationCodeEntity verificationCodeEntity
     *
     * @return risultato dell'operazione
     */
    private Mono<AddressBookService.SAVE_ADDRESS_RESULT> checkValidPecAndSendToDataVaultAndSaveInDynamodb(VerificationCodeEntity verificationCodeEntity, LegalChannelTypeDto legalChannelType)
    {
        verificationCodeEntity.setCodeValid(true);
        if (legalChannelType == LegalChannelTypeDto.PEC && !verificationCodeEntity.isPecValid()) {
            log.info("checkValidPecAndSendToDataVaultAndSaveInDynamodb set codeValid=true internalId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress());
            return this.dao.updateVerificationCodeIfExists(verificationCodeEntity)
                    .then(Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.PEC_VALIDATION_REQUIRED));
        } else {
            return sendToDataVaultAndSaveInDynamodb(verificationCodeEntity);
        }
    }

    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @return risultato dell'operazione
     */
    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> sendToDataVaultAndSaveInDynamodb(VerificationCodeEntity verificationCodeEntity)
    {
        // se real address non è passato, provo a recuperlo dal pec address
        String realaddress = verificationCodeEntity.getAddress();

        // se ancora non c'è, è un errore.
        if (!StringUtils.hasText(realaddress))
            return Mono.error(new PnInternalException("Addresso to save not found", PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_ADDRESS_NOT_FOUND));

        AddressBookEntity addressBookEntity = new AddressBookEntity(verificationCodeEntity.getRecipientId(), verificationCodeEntity.getAddressType(), verificationCodeEntity.getSenderId(), verificationCodeEntity.getChannelType());
        addressBookEntity.setAddresshash(verificationCodeEntity.getHashedAddress());
        String addressId = addressBookEntity.getAddressId();   //l'addressId è l'SK!
        log.info("saving address in datavault uid:{} hashedaddress:{} channel:{} legal:{}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress(), verificationCodeEntity.getChannelType(), verificationCodeEntity.getAddressType());
        return this.dataVaultClient.updateRecipientAddressByInternalId(verificationCodeEntity.getRecipientId(), addressId, realaddress)
                .then(verifiedAddressUtils.saveInDynamodb(addressBookEntity))
                .then(Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS));
    }

    /**
     * Imposta come pecValida
     *
     * @return risultato dell'operazione
     */
    public Mono<Void> markVerificationCodeAsPecValid(VerificationCodeEntity verificationCodeEntity)
    {

        log.info("Saving pec valid flag for requestId={}", verificationCodeEntity.getRequestId());
        verificationCodeEntity.setPecValid(true);

        return dao.updateVerificationCodeIfExists(verificationCodeEntity)
                .onErrorResume(throwable -> {
                    if (throwable instanceof ConditionalCheckFailedException ex)
                    {
                        // l'errore non dovrebbe aver senso, ho fatto il check 3 istruzioni più su. Cmq lo assorbo
                        log.error("Saving pec valid flag failed because item not found, probably by race condition, skipped save", ex);
                        return Mono.empty();
                    }
                    return Mono.error(throwable);
                });
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
                                                                                                                  LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String senderId) {
        String hashedaddress = hashAddress(realaddress);
        String addressType = getLegalType(legalChannelType);
        String vercode = getNewVerificationCode();
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        log.info("saving new verificationcode and send it to ext channel uid:{} hashedaddress:{} channel:{} newvercode:{}", recipientId, hashedaddress, channelType, vercode);
        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashedaddress, channelType, senderId, addressType, realaddress);
        verificationCode.setVerificationCode(vercode);
        verificationCode.setTtl(LocalDateTime.now().plus(getVerificationCodeTTL(legalChannelType)).atZone(ZoneId.systemDefault()).toEpochSecond());

        return removePreviousVerificationCode(verificationCode)
                .then(dao.saveVerificationCode(verificationCode))
                .flatMap(r -> pnExternalChannelClient.sendVerificationCode(recipientId, realaddress, legalChannelType, courtesyChannelType, verificationCode.getVerificationCode())
                                .flatMap(requestId -> {
                                    // aggiorno il requestId
                                    verificationCode.setRequestId(requestId);
                                    return dao.updateVerificationCodeIfExists(verificationCode);
                                })
                ).thenReturn(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
    }

    /**
     * Rimuove tutte le eventuali VC presenti precedentemente (per un certo senderId)
     *
     * @param verificationCodeEntity che si vuole inserire
     * @return void
     */
    private Mono<Void> removePreviousVerificationCode(VerificationCodeEntity verificationCodeEntity){
        return dao.getAllVerificationCodesByRecipient(verificationCodeEntity.getRecipientId(), verificationCodeEntity.getAddressType())
                .flatMap(oldVerificationCode -> {
                    if (Objects.equals(oldVerificationCode.getSenderId(), verificationCodeEntity.getSenderId())
                        && Objects.equals(oldVerificationCode.getChannelType(), verificationCodeEntity.getChannelType()))
                    {
                        return dao.deleteVerificationCode(oldVerificationCode);
                    }
                    else {
                        return Mono.empty();
                    }
                }).collectList()
                .then();
    }

    private Duration getVerificationCodeTTL(LegalChannelTypeDto legalChannelType){
        if (legalChannelType != null)
            return pnUserattributesConfig.getVerificationCodeLegalTTL();
        else
            return pnUserattributesConfig.getVerificationCodeCourtesyTTL();
    }

    private Mono<VerificationCodeEntity> manageAttempts(VerificationCodeEntity verificationCodeEntity, String verificationCode, LegalChannelTypeDto legalChannelType) {
        if (verificationCodeEntity.getLastModified().isBefore(Instant.now().minus(getVerificationCodeTTL(legalChannelType)))) {
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



    public Mono<Boolean> validateHashedAddress(String recipientId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto){
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        validateAddress(legalChannelType, courtesyChannelType, addressVerificationDto);
        String hashedAddress;
        if (StringUtils.hasText(addressVerificationDto.getValue())) {
            hashedAddress = hashAddress(addressVerificationDto.getValue());
            return dao.validateHashedAddress(recipientId, hashedAddress, channelType)
                    .map(res -> res == AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED);
        }
        else
            return Mono.just(false);
    }


    private void validateAddress(LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto) {
        // se è specificato il requestId, non mi interessa il value. Deve però essere presente il verification code
        if (addressVerificationDto.getRequestId() != null)
        {
            if (StringUtils.hasText(addressVerificationDto.getVerificationCode()))
                return;
            else
                throw new PnInvalidInputException(PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "verificationCode");
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
