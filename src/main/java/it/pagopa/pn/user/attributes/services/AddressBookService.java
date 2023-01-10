package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.UserStatusResponse;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.*;

@Service
@Slf4j
public class AddressBookService {

    private static final int VERIFICATION_CODE_TTL_MINUTES = 10;
    private final AddressBookDao dao;
    private final PnDataVaultClient dataVaultClient;
    private final PnExternalChannelClient pnExternalChannelClient;
    private final PnExternalRegistryIoClient pnExternalRegistryClient;
    private final AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto;
    private final AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;
    private final IONotificationService ioNotificationService;

    SecureRandom rnd = new SecureRandom();

    public enum SAVE_ADDRESS_RESULT{
        SUCCESS,
        CODE_VERIFICATION_REQUIRED
    }


    public AddressBookService(AddressBookDao dao,
                              PnDataVaultClient dataVaultClient,
                              PnExternalChannelClient pnExternalChannelClient, PnExternalRegistryIoClient pnExternalRegistryClient, AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto, IONotificationService ioNotificationService) {
        this.dao = dao;
        this.dataVaultClient = dataVaultClient;
        this.pnExternalChannelClient = pnExternalChannelClient;
        this.pnExternalRegistryClient = pnExternalRegistryClient;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
        this.ioNotificationService = ioNotificationService;
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
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> enrichWithAppIo(recipientId, list))
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .collectList()
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> enrichWithAppIo(recipientId, list))
                .flatMapIterable(x -> x);
    }

    public Mono<Boolean> isAppIoEnabledByRecipient(String recipientId)
    {
        return dao.getAllAddressesByRecipient(recipientId, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .filter(x -> x.getChannelType().equals(CourtesyChannelTypeDto.APPIO.getValue()))
                .take(1).next()
                .map(x -> true)
                .defaultIfEmpty(false);
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
                .flatMap(list ->  deanonimizeLegal(recipientId, list))
                .flatMapIterable(x -> x);
    }

    /**
     * Lista indirizzi in base al recipient
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .collectList()
                .flatMap(list -> deanonimizeLegal(recipientId, list))
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi LEGALI e di CORTESIA per il recipient
     *
     * @param recipientId id utente
     * @return oggetto contenente le liste LEGALI e di CORTESIA
     */
    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, null).collectList()
                .zipWhen(list -> dataVaultClient.getRecipientAddressesByInternalId(recipientId),
                (list, addresses) -> {
                    UserAddressesDto dto = new UserAddressesDto();
                    dto.setCourtesy(new ArrayList<>());
                    dto.setLegal(new ArrayList<>());

                    list.forEach(ent -> {
                        // Nel caso di APPIO, non esiste un address da risolvere in data-vault
                        String realaddress;
                        if (ent.getChannelType().equals(CourtesyChannelTypeDto.APPIO.getValue()))
                            realaddress = ent.getAppioStatus();
                        else
                            realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre,

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
                .switchIfEmpty(Mono.defer(() -> {
                    UserAddressesDto dto = new UserAddressesDto();
                    dto.setCourtesy(new ArrayList<>());
                    dto.setLegal(new ArrayList<>());
                    return Mono.just(dto);
                }))
                .zipWhen(dto -> enrichWithAppIo(recipientId, dto.getCourtesy()),
                        (dto, courtesy) -> {
                            dto.setCourtesy(courtesy);
                            return dto;
                        }
                );
    }

    private Mono<List<CourtesyDigitalAddressDto>> enrichWithAppIo(String recipientId, List<CourtesyDigitalAddressDto> source)
    {
        log.info("enrichWithAppIo recipientId={}", recipientId);
        // devo controllare che l'APPIO non sia presente tra i risultati.
        // se è presente, è perchè è abilitata, e quindi non serve fare altro.
        // altrimenti, devo chiedere al BE di IO se l'utente è un utente di APPIO o no
        Optional<CourtesyDigitalAddressDto> appioAddress = source.stream().filter(x -> x.getChannelType().getValue().equals(CourtesyChannelTypeDto.APPIO.getValue())).findFirst();
        if (appioAddress.isEmpty())
        {
            // mi ricavo il CF da datavault, poi lo uso per recuperare se è un utente valido
            return this.pnExternalRegistryClient.checkValidUsers(recipientId)
                    .map(user -> {
                        // se non è attivo su IO, ritorno il dto SENZA APPIO
                        if (user.getStatus() == UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE)
                        {
                            log.info("enrichWithAppIo appio is not available, not returning appio courtesy recipientId={}", recipientId);
                            return source;
                        }
                        else if (user.getStatus() == UserStatusResponse.StatusEnum.ERROR)
                        {
                            throw new PnInternalException("IO user check status failed", ERROR_CODE_IO_ERROR);
                        }
                        else
                        {
                            log.info("enrichWithAppIo appio is available, adding appio courtesy as disabled recipientId={}", recipientId);
                            // altrimenti, vuol dire che è presente ma disabilitato.
                            // si noti infatti che NON posso fidarmi del mio flag di disabilitato,
                            // perchè quel flag "DISABLED" da noi in PN può rappresentare sia il "APPIO non ATTIVO", sia "APPIO attivo ma PN disablitato"
                            CourtesyDigitalAddressDto add = new CourtesyDigitalAddressDto();
                            add.setValue(AddressBookEntity.APP_IO_DISABLED);
                            add.setRecipientId(recipientId);
                            add.setChannelType(CourtesyChannelTypeDto.APPIO);
                            add.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
                            add.setSenderId(AddressBookEntity.SENDER_ID_DEFAULT);
                            source.add(add);
                            return source;
                        }
                    });
        }
        else
        {
            log.info("enrichWithAppIo appio courtesy is already enabled recipientId={}", recipientId);
            return Mono.just(source);
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

        if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
            // le richieste da APPIO non hanno "indirizzo", posso procedere con l salvataggio in dynamodb,
            // senza dover passare per la creazione di un VC
            // Devo cmq creare un VA con il channelType
            return sendToIoActivationServiceAndSaveInDynamodb(recipientId, legal, senderId, channelType)
                    .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
        }
        else {
            return addressVerificationDto
                    .zipWhen(r -> dao.validateHashedAddress(recipientId, hashAddress(r.getValue()), channelType)
                            , (r, alreadyverifiedoutcome) -> new Object() {
                                public final String verificationCode = r.getVerificationCode();
                                public final String realaddress = r.getValue();
                                public final AddressBookDao.CHECK_RESULT alreadyverifiedOutcome = alreadyverifiedoutcome;
                            })
                    .flatMap(res -> {
                        if (res.alreadyverifiedOutcome == AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED) {
                            // l'indirizzo risulta già verificato precedentemente, posso procedere con il salvataggio in data-vault,
                            // senza dover passare per la creazione di un VC
                            // Devo cmq creare un VA con il channelType
                            return this.sendToDataVaultAndSaveInDynamodb(recipientId, res.realaddress, legal, senderId, channelType);
                        } else {
                            // l'indirizzo non è verificato. Ho due casi possibili:
                            if (!StringUtils.hasText(res.verificationCode)) {
                                // CASO A: non mi viene passato un codice verifica
                                return this.saveInDynamodbNewVerificationCodeAndSendToExternalChannel(recipientId, res.realaddress, legalChannelType, courtesyChannelType);
                            } else {
                                // CASO B: ho un codice di verifica da validare e poi procedere.
                                return this.validateVerificationCodeAndSendToDataVault(recipientId, res.verificationCode, res.realaddress, legal, senderId, channelType);
                            }

                        }
                    });
        }
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
        log.info("deleteAddressBook recipientId={} senderId={} legalChannelType={} courtesyChannelType={}", recipientId, senderId, legalChannelType, courtesyChannelType);
        String legal = getLegalType(legalChannelType);
        String channelType = getChannelType(legalChannelType, courtesyChannelType);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);

        if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
            // le richieste da APPIO hanno una gestione complessa dedicata
            return deleteAddressBookAppIo(addressBookEntity);
        }
        else {
            return dataVaultClient.deleteRecipientAddressByInternalId(recipientId, addressBookEntity.getAddressId())
                    .then(dao.deleteAddressBook(recipientId, senderId, legal, channelType));
        }
    }

    @NotNull
    private Mono<Object> deleteAddressBookAppIo(AddressBookEntity addressBookEntity) {
        // le richieste da APPIO non hanno "indirizzo", posso procedere con l'eliminazione in dynamodb, che però è solo logica, quindi vado a impostare il flag a FALSE
        AtomicBoolean waspresent = new AtomicBoolean(true);
        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_DISABLED);

        return dao.getAddressBook(addressBookEntity)
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.info("Never activated, proceeding with io-deactivation");
                    waspresent.set(false);
                    return addressBookEntity;
                }))
                .then(saveInDynamodb(addressBookEntity))
                .then(this.pnExternalRegistryClient.upsertServiceActivation(addressBookEntity.getRecipientId(), false))
                .onErrorResume(throwable -> {
                    if (waspresent.get()) {
                        log.error("Saving to io-activation-service failed, re-adding to addressbook appio channeltype");
                        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_ENABLED);
                        return saveInDynamodb(addressBookEntity)
                                .then(Mono.error(throwable));
                    } else
                        return Mono.error(throwable);
                })
                .flatMap(activated -> {
                    if (Boolean.TRUE.equals(activated)) {
                        log.error("outcome io-status is activated, re-adding to addressbook appio channeltype");
                        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_ENABLED);
                        return saveInDynamodb(addressBookEntity)
                                .then(Mono.error(new PnInternalException("IO deactivation failed", ERROR_CODE_IO_DEACTIVATION_FAILED)));
                    } else {
                        log.info("outcome io-status is not activated, deletion successful");
                        return Mono.just(new Object());
                    }
                })
                .then(Mono.just(new Object()));
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
                        return Mono.error(new PnInvalidVerificationCodeException());
                    if (r.getLastModified().isBefore(Instant.now().minus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES)))
                        return Mono.error(new PnInvalidVerificationCodeException());

                    log.info("Verification code validated uid:{} hashedaddress:{} channel:{} addrtype:{}", recipientId, hashedaddress, channelType, legal);
                    return sendToDataVaultAndSaveInDynamodb(recipientId, realaddress, legal, senderId, channelType);
                })
                .switchIfEmpty(Mono.error(new PnInvalidVerificationCodeException()));
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
        verificationCode.setTtl(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_TTL_MINUTES).atZone(ZoneId.systemDefault()).toEpochSecond());

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
        addressBookEntity.setAddresshash(hashedaddress);
        String addressId = addressBookEntity.getAddressId();   //l'addressId è l'SK!
        log.info("saving address in datavault uid:{} hashedaddress:{} channel:{} legal:{}", recipientId, hashedaddress, channelType, legal);
        return this.dataVaultClient.updateRecipientAddressByInternalId(recipientId, addressId, realaddress)
                .then(saveInDynamodb(addressBookEntity))
                .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
    }

    /**
     * Invia al datavault e se tutto OK salva in dynamodb l'indirizzo offuscato
     *
     * @param recipientId idutente
     * @param legal tipologia
     * @param senderId eventuale preferenza mittente
     * @param channelType tipologia canale
     * @return risultato dell'operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> sendToIoActivationServiceAndSaveInDynamodb(String recipientId, String legal, String senderId, String channelType)
    {
        //NB: il metodo deve anche leggere l'eventuale AB presente, perchè deve poi schedulare l'invio di eventuali notifiche "recenti" (Xgg), e c'è bisogno di sapere se
        // il flag era già stato impostato nel periodo tra ORA e ORA-Xgg, perchè vuol dire che le eventuali notifiche fino a quel momento sono già state notificate via IO
        // Morale della favola: devo trovare il MAX tra "ORA-Xgg" e "lastUpdate" se presente.
        // NB: non devo sovrascrivere se già presente

        log.info("sendToIoActivationServiceAndSaveInDynamodb sending to io-activation-service and save in db uid:{} channel:{} legal:{}", recipientId, channelType, legal);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);
        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_ENABLED);

        return dao.getAddressBook(addressBookEntity)        //(1) chiedo lo stato corrente di APPIO per l'utente
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    // (2) se non lo trovo, ne creo uno fittizio di disabilitato, con data di ultima modifica molto vecchia
                    AddressBookEntity defAddressBook = new AddressBookEntity(recipientId, legal, senderId, channelType);
                    defAddressBook.setAddresshash(AddressBookEntity.APP_IO_DISABLED);   // non trovarla equivale a disabilitata
                    defAddressBook.setLastModified(Instant.EPOCH);  // setto la data di ultima modifica ad un valore "molto" indietro nel tempo
                    return defAddressBook;
                }))
                .zipWhen(ab -> {
                    // (3) se era già presente ed abilitato, non c'è altro da fare, altrimenti va creata (caso più probabile)
                    if (ab.getAddresshash().equals(AddressBookEntity.APP_IO_ENABLED))
                    {
                        // non c'è niente da fare, era già presente un record con APPIO abilitata
                        return Mono.just(ab);
                    }
                    else
                    {
                        // non era presente, devo ovviamente salvarlo
                        return saveInDynamodb(addressBookEntity)
                                .then(Mono.just(ab));
                    }
                }, (ab, r) -> ab)
                .zipWhen(ab -> this.pnExternalRegistryClient.upsertServiceActivation(recipientId, true)
                                .onErrorResume(throwable -> {
                                    log.error("Saving to io-activation-service failed, deleting from addressbook appio channeltype");
                                    // se da errore l'invocazione a io-activation-service, faccio "rollback" sul salvataggio in dynamo-db
                                    return dao.deleteAddressBook(recipientId, senderId, legal, channelType)
                                            .then(Mono.error(throwable));
                                })
                        , (previousAddressBook0, activated0) -> new Object(){
                    public final AddressBookEntity previousAddressBook=previousAddressBook0;
                    public final Boolean activated = activated0;
                })
                .flatMap(zipRes -> {
                    if (Boolean.TRUE.equals(zipRes.activated))
                    {
                        log.info("outcome io-status is activated, creation successful");
                        if (zipRes.previousAddressBook.getAddresshash().equals(AddressBookEntity.APP_IO_DISABLED))
                        {
                            return ioNotificationService.scheduleCheckNotificationToSendAfterIOActivation(recipientId, zipRes.previousAddressBook.getLastModified())
                                    .then(Mono.just(new Object()));
                        }
                        else
                        {
                            log.info("previous address-book io-status was already enabled, no need to schedule checknotificationtosend");
                            return Mono.just(new Object());
                        }
                    }
                    else
                    {
                        log.error("outcome io-status is not-activated, re-deleting to addressbook appio channeltype");
                        return dao.deleteAddressBook(recipientId, senderId, legal, channelType)
                                .then(Mono.error(new PnInternalException("IO activation failed", ERROR_CODE_IO_ACTIVATION_FAILED)));
                    }
                })
                .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
    }

    /**
     * Salva in dynamodb l'id offuscato
     *
     * @param addressBook addressBook da salvare, COMPLETO di hashedaddress impostato
     * @return nd
     */
    private Mono<Void> saveInDynamodb(AddressBookEntity addressBook){
        log.info("saving address in db uid:{} hashedaddress:{} channel:{} legal:{}", addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType(), addressBook.getAddressType());

        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());

        return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity);
    }


    private Mono<List<CourtesyDigitalAddressDto>> deanonimizeCourtesy(String recipientId, List<AddressBookEntity> list)
    {
        return dataVaultClient.getRecipientAddressesByInternalId(recipientId)
                .map(addresses -> {
                    List<CourtesyDigitalAddressDto> res = new ArrayList<>();
                    list.forEach(ent -> {
                        // Nel caso di APPIO, non esiste un address da risolvere in data-vault
                        String realaddress;
                        if (ent.getChannelType().equals(CourtesyChannelTypeDto.APPIO.getValue()))
                            realaddress = ent.getAppioStatus();
                        else
                            realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre, ce l'ho messo io

                        CourtesyDigitalAddressDto add = addressBookEntityToDto.toDto(ent);
                        add.setValue(realaddress);
                        res.add(add);
                    });

                    return res;
                });
    }


    private Mono<List<LegalDigitalAddressDto>> deanonimizeLegal(String recipientId, List<AddressBookEntity> list)
    {
        return dataVaultClient.getRecipientAddressesByInternalId(recipientId)
                .map(addresses -> {
                    List<LegalDigitalAddressDto> res = new ArrayList<>();
                    list.forEach(ent -> {
                        String realaddress = addresses.getAddresses().get(ent.getAddressId()).getValue();  // mi aspetto che ci sia sempre, ce l'ho messo io

                        LegalDigitalAddressDto add = legalDigitalAddressToDto.toDto(ent);
                        add.setValue(realaddress);
                        res.add(add);
                    });

                    return res;
                });
    }
}
