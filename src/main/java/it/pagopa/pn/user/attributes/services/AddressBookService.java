package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AddressBookService {
    // Nel branch feature/PN-1283 il controllo del codice di verifica tramite external channell non viene fatto.
    // Temporaneamente il controllo viene fatto confrontando il codice immesso con una stringa costante
    public static final String VERIFICATION_CODE_OK = "12345";

    private final AddressBookDao dao;
    private final PnDataVaultClient dataVaultClient;
    private final PnExternalChannelClient pnExternalChannelClient;
    private final AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto;
    private final AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;

    public enum SAVE_ADDRESS_RESULT{
        SUCCESS,
        CODE_VERIFICATION_REQUIRED
    }


    public AddressBookService(AddressBookDao dao,
                              PnDataVaultClient dataVaultClient,
                              PnExternalChannelClient pnExternalChannelClient, AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto) {
        this.dao = dao;
        this.dataVaultClient = dataVaultClient;
        this.pnExternalChannelClient = pnExternalChannelClient;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
    }


    /**
     * Il metodo si occupa di salvare un indirizzo di tipo LEGALE
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param legalChannelType tipologia canale legale
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, Mono<AddressVerificationDto> addressVerificationDto) {
        return saveAddressBook(recipientId, senderId, legalChannelType, null,  addressVerificationDto);
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo CORTESIA
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param courtesyChannelType tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType, Mono<AddressVerificationDto> addressVerificationDto) {
        return saveAddressBook(recipientId, senderId, null, courtesyChannelType,  addressVerificationDto);
    }

    /**
     * Elimina un indirizzo di tipo LEGALE
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @param legalChannelType tipo canale legale
     * @return nd
     */
    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType) {
        return deleteAddressBook(recipientId, senderId, legalChannelType, null);
    }

    /**
     * Elimina un indirizzo di tipo CORTESIA
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @param courtesyChannelType tipo canale cortesia
     * @return nd
     */
    public Mono<Object> deleteCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType) {
        return deleteAddressBook(recipientId, senderId, null, courtesyChannelType);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base a recipient e sender id
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @return lista indirizzi di cortesia
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipientAndSender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .collectList()
                .zipWhen(list -> dataVaultClient.getRecipientAddressesByInternalId(recipientId),
                        (list, addresses) -> {
                            List<CourtesyDigitalAddressDto> res = new ArrayList<>();
                            list.forEach(ent -> {
                                String realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre, ce l'ho messo io
                                CourtesyDigitalAddressDto add = addressBookEntityToDto.toDto(ent);
                                add.setValue(realaddress);
                                res.add(add);
                            });

                            return res;
                        })
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return getCourtesyAddressByRecipientAndSender(recipientId, null);
    }

    /**
     * Ritorna gli indirizzi LEGALI per li recipitent e il sender id
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @return lista indirizzi
     */
    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipientAndSender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .collectList()
                .zipWhen(list -> dataVaultClient.getRecipientAddressesByInternalId(recipientId),
                        (list, addresses) -> {
                            List<LegalDigitalAddressDto> res = new ArrayList<>();
                            list.forEach(ent -> {
                                String realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre, ce l'ho messo io
                                LegalDigitalAddressDto add = legalDigitalAddressToDto.toDto(ent);
                                add.setValue(realaddress);
                                res.add(add);
                            });

                            return res;
                        })
                .flatMapIterable(x -> x);
    }

    /**
     * Lista indirizzi in base al recipient
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return this.getLegalAddressByRecipientAndSender(recipientId, null);
    }

    /**
     * Ritorna gli indirizzi LEGALI e di CORTESIA per il recipient
     *
     * @param recipientId id utente
     * @return oggetto contenente le liste LEGALI e di CORTESIA
     */
    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId).collectList()
                .zipWhen(list -> dataVaultClient.getRecipientAddressesByInternalId(recipientId),
                (list, addresses) -> {
                    UserAddressesDto dto = new UserAddressesDto();

                    list.forEach(ent -> {
                        String realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre,
                        if (ent.getAddressType().equals(LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())) {
                            LegalDigitalAddressDto add = legalDigitalAddressToDto.toDto(ent);
                            add.setValue(realaddress);
                            dto.addLegalItem(add);
                        }
                        else {
                            CourtesyDigitalAddressDto add = addressBookEntityToDto.toDto(ent);
                            add.setValue(realaddress);
                            dto.addCourtesyItem(add);
                        }
                    });
                    return dto;
                })
                .switchIfEmpty(Mono.just(new UserAddressesDto()));
    }

    /**
     * Genera un nuovo codice
     *
     * @return il codice generato
     */
    private String getNewVerificationCode() {
        // FIXME: generare veramente un nuovo codice
        log.debug("generated a new verificationCode: {}", AddressBookService.VERIFICATION_CODE_OK);
        return AddressBookService.VERIFICATION_CODE_OK;
    }

    /**
     * Ricava il tipo di channel in base agli enum.
     * Ci si aspetta che solo un parametro sia valorizzato per volta
     *
     * @param legalChannelType eventuale channelType legale
     * @param courtesyChannelType eventuale channelType cortesia
     * @return stringa rappresentante il channelType
     */
    private String getChannelType(LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType)
    {
        return legalChannelType!=null?legalChannelType.getValue():courtesyChannelType.getValue();
    }

    /**
     * Ricava il tipo di canale (legale o cortesia)
     *
     * @param legalChannelType eventuale channelType legale. Se null, si intende di cortesia
     * @return stringa rappresentante il tipo di canale
     */
    private String getLegalType(LegalChannelTypeDto legalChannelType)
    {
        return legalChannelType!=null?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue();
    }

    /**
     * Wrap dello sha per rendere più facile capire dove viene usato
     * @param realaddress indirizzo da hashare
     * @return hash dell'indirizzo
     */
    private String hashAddress(@NonNull String realaddress)
    {
        return DigestUtils.sha256Hex(realaddress);
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
     * @param legalChannelType tipologia canale legale
     * @param courtesyChannelType tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> saveAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, Mono<AddressVerificationDto> addressVerificationDto) {
        String legal = getLegalType(legalChannelType);
        String channelType = getChannelType(legalChannelType, courtesyChannelType);

        return addressVerificationDto
                .zipWhen(r -> dao.validateHashedAddress(recipientId, hashAddress(r.getValue()))
                        ,(r, alreadyverifiedoutcome) -> new Object(){
                            public final String verificationCode = r.getVerificationCode();
                            public final String realaddress = r.getValue();
                            public final AddressBookDao.CHECK_RESULT alreadyverifiedOutcome = alreadyverifiedoutcome;
                        })
                .flatMap(res -> {
                    if (res.alreadyverifiedOutcome == AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED)
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
                            return this.saveInDynamodbNewVerificationCodeAndSendToExternalChannel(recipientId, res.realaddress, legalChannelType, courtesyChannelType);
                        }
                        else
                        {
                            // CASO B: ho un codice di verifica da validare e poi procedere.
                            return this.validateVerificationCodeAndSendToDataVault(recipientId, res.verificationCode, res.realaddress, legal, senderId, channelType);
                        }

                    }
                });
    }

    /**
     * Elimina un indirizzo
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @param legalChannelType eventuale canale legale
     * @param courtesyChannelType eventuale canale cortesia
     * @return nd
     */
    private Mono<Object> deleteAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType) {
        String legal = getLegalType(legalChannelType);
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);
        return  dataVaultClient.deleteRecipientAddressByInternalId(recipientId, addressBookEntity.getAddressId())
                .then(dao.deleteAddressBook(recipientId, senderId, legal, channelType));
    }

    /**
     * Valida un codice di verifica e lo anonimizza
     *
     * @param recipientId id utente
     * @param verificationCode codice verifica
     * @param realaddress indirizzo da anonimizzare
     * @param legal tipo canale legale
     * @param senderId id mittente
     * @param channelType tipo canale
     * @return risultato dell'operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> validateVerificationCodeAndSendToDataVault(String recipientId, String verificationCode, String realaddress, String legal, String senderId, String channelType) {
        String hashedaddress = hashAddress(realaddress);
        log.info("validating code uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, hashedaddress, channelType, legal);
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(recipientId, hashedaddress, channelType);
        return dao.getVerificationCode(verificationCodeEntity)
                .flatMap(r -> {
                    if (!r.getVerificationCode().equals(verificationCode))
                        return Mono.error(new InvalidVerificationCodeException());

                    log.info("Verification code validated uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, hashedaddress, channelType, legal);
                    return sendToDataVaultAndSaveInDynamodb(recipientId, realaddress, legal, senderId, channelType);
                })
                .switchIfEmpty(Mono.error(new InvalidVerificationCodeException()));
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
    private Mono<SAVE_ADDRESS_RESULT> saveInDynamodbNewVerificationCodeAndSendToExternalChannel(String recipientId, String realaddress, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType) {
        String hashedaddress = hashAddress(realaddress);
        String vercode = getNewVerificationCode();
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        log.info("saving new verificationcode and send it to ext channel uid:{} hashedaddress:{} channel:{} newvercode:{}", recipientId, hashedaddress, channelType, vercode);
        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashedaddress, channelType);
        verificationCode.setVerificationCode(vercode);
        verificationCode.setTtl(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toEpochSecond());

        return dao.saveVerificationCode(verificationCode)
                .zipWhen(r -> pnExternalChannelClient.sendVerificationCode(recipientId, realaddress, legalChannelType, courtesyChannelType, verificationCode.getVerificationCode())
                                .thenReturn("OK")
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
     * @return risultato dell'operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> sendToDataVaultAndSaveInDynamodb(String recipientId, String realaddress, String legal, String senderId, String channelType)
    {
        String hashedaddress = hashAddress(realaddress);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);
        String addressId = addressBookEntity.getAddressId();   //l'addressId è l'SK!
        log.info("saving address in datavault uid:{} hashedaddress:{} channel:{} legal:{}", recipientId, hashedaddress, channelType, legal);
        return this.dataVaultClient.updateRecipientAddressByInternalId(recipientId, addressId, realaddress)
                .then(saveInDynamodb(recipientId, hashedaddress, legal, senderId, channelType))
                .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
    }

    /**
     * Salva in dynamodb l'id offuscato
     *
     * @param recipientId id utente
     * @param hashedaddress hash indirizzo
     * @param legal tipo indirizzo legale
     * @param senderId id mittente
     * @param channelType tipo canale
     * @return nd
     */
    private Mono<Void> saveInDynamodb(String recipientId, String hashedaddress, String legal, String senderId, String channelType){
        log.info("saving address in db uid:{} hashedaddress:{} channel:{} legal:{}", recipientId, hashedaddress, channelType, legal);
        AddressBookEntity addressBook = new AddressBookEntity(recipientId, legal, senderId, channelType);

        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(recipientId, hashedaddress, channelType);

        return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity);
    }
}
