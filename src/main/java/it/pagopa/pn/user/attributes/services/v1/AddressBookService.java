package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.UserAddressesDto;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityListToUserAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerifiedAddressEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Slf4j
public class AddressBookService {
    // Nel branch feature/PN-1283 il controllo del codice di verifica tramite external channell non viene fatto.
    // Temporaneamente il controllo viene fatto confrontando il codice immesso con una stringa costante
    public static final String VERIFICATION_CODE_OK = "12345";
    public static final String VERIFICATION_CODE_MISMATCH = "Verification code mismatch";

    private final AddressBookDao dao;
    private final PnDataVaultClient dataVaultClient;
    private final PnExternalChannelClient pnExternalChannelClient;
    private final AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto;
    private final AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;
    private final AddressBookEntityListToUserAddressDtoMapper addressBookEntityListToDto;

    public enum SAVE_ADDRESS_RESULT{
        SUCCESS,
        CODE_VERIFICATION_REQUIRED
    }


    public AddressBookService(AddressBookDao dao,
                              PnDataVaultClient dataVaultClient,
                              PnExternalChannelClient pnExternalChannelClient, AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto,
                              AddressBookEntityListToUserAddressDtoMapper addressBookEntityListToDto) {
        this.dao = dao;
        this.dataVaultClient = dataVaultClient;
        this.pnExternalChannelClient = pnExternalChannelClient;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
        this.addressBookEntityListToDto = addressBookEntityListToDto;
    }


    /**
     * Il metodo si occupa di salvare un indirizzo, gestendo di fatto le varie casisitiche
     * Il metodo prevede:
     * - Cercare in DB se esiste già un indirizzo verificato (senza considerare il channelType che può essere diverso), con la stessa SHA
     * - Se lo trova, salta la parte di invio codice e invoca il datavault per anonimizzarlo e salvare il valore anonimizzato in DB
     * - Se non lo trova, ho due casi possibili:
     * -- CASO A: non mi viene passato un codice verifica: è il caso più comune in cui l'utente crea un nuovo indirizzo per la prima volta.
     *    - Salvo un nuovo VerificationCode e procedo all'invio del codice di verifica.
     * -- CASO B: mi viene passato un codice di verifica (valido): è il secondo step della procedura, in cui l'utente convalida l'indirizzo.
     *    - Recupero il VerificationCode, e lo valido.
     *    - Se valido, invoco il datavault per anonimizzarlo e salvare il valore anonimizzato in DB.
     *
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param isLegal tipologia
     * @param channelType tipologia canale
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveAddressBook(String recipientId, String senderId, boolean isLegal,  String channelType, Mono<AddressVerificationDto> addressVerificationDto) {
        String legal = isLegal?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue();
        return addressVerificationDto
                .zipWhen(r -> dao.getVerifiedAddress(recipientId, hashAddress(r.getValue()))
                        .map(v -> true)
                        .defaultIfEmpty(false)
                ,(r, isalreadyverified) -> new Object(){
                    public final String verificationCode = r.getVerificationCode();
                    public final String realaddress = r.getValue();
                    public final boolean isAlreadyverified = isalreadyverified;
                })
                .flatMap(res -> {
                    if (res.isAlreadyverified)
                    {
                        // l'indirizzo risulta già verificato precedentemente, posso procedere con il salvataggio in data-vault,
                        // senza dover passare per la creazione di un VC
                        // Devo cmq creare un VA con il channelType
                        return this.sendToDataVaultAndSaveInDynamodb(recipientId, res.realaddress, legal, senderId, channelType);
                    }
                    else
                    {
                        // l'indirizzo non è verificato. Ho due casi possibili:
                        if (!StringUtils.hasText(res.verificationCode))
                        {
                            // CASO A: non mi viene passato un codice verifica
                            return this.saveInDynamodbNewVerificationCodeAndSendToExternalChannel(recipientId, res.realaddress, channelType);
                        }
                        else
                        {
                            // CASO B: ho un codice di verifica da validare e poi procedere.
                            return this.validateVerificationCodeAndSendToDataVault(recipientId, res.verificationCode, res.realaddress, legal, senderId, channelType);
                        }

                    }
                });
    }


    public Mono<Object> deleteAddressBook(String recipientId, String senderId, boolean isLegal,  String channelType) {
        String legal = isLegal?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue();
        return dao.deleteAddressBook(recipientId, senderId, legal, channelType);
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressBySender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .map(addressBookEntityToDto::toDto);
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getAddresses(recipientId, null, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .map(addressBookEntityToDto::toDto);
    }

    public Flux<LegalDigitalAddressDto> getLegalAddressBySender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .map(legalDigitalAddressToDto::toDto);

    }

    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return dao.getAddresses(recipientId, null, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .map(legalDigitalAddressToDto::toDto);

    }

    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId).collectList()
                .map(addressBookEntityListToDto::toDto);
    }


    private String getNewVerificationCode() {
        log.debug("generated a new verificationCode: {}", AddressBookService.VERIFICATION_CODE_OK);
        return AddressBookService.VERIFICATION_CODE_OK;
    }

    private String hashAddress(@NonNull String realaddress)
    {
        return DigestUtils.sha256Hex(realaddress);
    }


    private Mono<SAVE_ADDRESS_RESULT> validateVerificationCodeAndSendToDataVault(String recipientId, String verificationCode, String realaddress, String legal, String senderId, String channelType) {
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(recipientId, hashAddress(realaddress), channelType);
        return dao.getVerificationCode(verificationCodeEntity)
                .flatMap(r -> {
                    if (!r.getVerificationCode().equals(verificationCode))
                        return Mono.error(new InvalidVerificationCodeException());

                    log.info("Verification code validated uid:{} address:{} channel:{} addrtype:{}", recipientId, realaddress, channelType, legal);
                    return sendToDataVaultAndSaveInDynamodb(recipientId, realaddress, legal, senderId, channelType);
                });
    }

    private Mono<SAVE_ADDRESS_RESULT> saveInDynamodbNewVerificationCodeAndSendToExternalChannel(String recipientId, String realaddress, String channelType) {
        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashAddress(realaddress), channelType);
        verificationCode.setVerificationCode(getNewVerificationCode());
        verificationCode.setTtl(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toEpochSecond());

        return dao.saveVerificationCode(verificationCode)
                .zipWhen(r -> pnExternalChannelClient.sendVerificationCode(realaddress, channelType, verificationCode.getVerificationCode())
                        ,(r, a) -> SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
    }

    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @param recipientId idutente
     * @param realaddress indirizzo da salvare
     * @param legal tipologia
     * @param senderId eventuale preferenza mittente
     * @param channelType tipologia canale
     * @return success
     */
    private Mono<SAVE_ADDRESS_RESULT> sendToDataVaultAndSaveInDynamodb(String recipientId, String realaddress, String legal, String senderId, String channelType)
    {
        String addressId = UUID.randomUUID().toString();
        return this.dataVaultClient.updateRecipientAddressByInternalId(recipientId, addressId, realaddress).zipWhen(r -> {
            AddressBookEntity addressBook = new AddressBookEntity(recipientId, legal, senderId, channelType);
            addressBook.setAddressId(addressId);

            VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(recipientId, hashAddress(realaddress), channelType);

            return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity);
        })
        .map(r -> SAVE_ADDRESS_RESULT.SUCCESS);
    }
}
