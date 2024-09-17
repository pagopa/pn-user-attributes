package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.LegalDigitalAddressDtoToLegalAndUnverifiedDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.VerificationCodeEntityToLegalAndUnverifiedDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnSelfcareClient;
import it.pagopa.pn.user.attributes.services.utils.AppIOUtils;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.dto.PaSummary;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.user.attributes.utils.PgUtils;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_SENDERIDNOTROOT;
import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;

@Service
@Slf4j
public class AddressBookService {

    private final AddressBookDao dao;
    private final PnDataVaultClient dataVaultClient;
    private final AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto;
    private final AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;
    private final PnSelfcareClient pnSelfcareClient;
    private final VerificationCodeUtils verificationCodeUtils;
    private final AppIOUtils appIOUtils;

    private final PnExternalRegistryClient externalRegistryClient;

    private final PnUserattributesConfig pnUserattributesConfig;

    public enum SAVE_ADDRESS_RESULT {
        SUCCESS,
        CODE_VERIFICATION_REQUIRED,
        PEC_VALIDATION_REQUIRED
    }


    public AddressBookService(AddressBookDao dao,
                              PnDataVaultClient dataVaultClient,
                              AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto,
                              PnSelfcareClient pnSelfcareClient, VerificationCodeUtils verificationCodeUtils,
                              AppIOUtils appIOUtils, PnExternalRegistryClient externalRegistryClient,
                              PnUserattributesConfig pnUserattributesConfig) {

        this.pnUserattributesConfig = pnUserattributesConfig;
        this.dao = dao;
        this.dataVaultClient = dataVaultClient;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
        this.pnSelfcareClient = pnSelfcareClient;
        this.verificationCodeUtils = verificationCodeUtils;
        this.appIOUtils = appIOUtils;
        this.externalRegistryClient = externalRegistryClient;
    }


    /**
     * Il metodo si occupa di salvare un indirizzo di tipo LEGALE
     *
     * @param recipientId            id utente
     * @param senderId               eventuale id PA
     * @param legalChannelType       tipologia canale legale
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, AddressVerificationDto addressVerificationDto, List<TransactDeleteItemEnhancedRequest> deleteItemResponse) {
        return saveAddressBook(recipientId, senderId, legalChannelType, null, addressVerificationDto, deleteItemResponse);
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo LEGALE. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId            id utente
     * @param senderId               eventuale id PA
     * @param legalChannelType       tipologia canale legale
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @param pnCxType               user's type
     * @param pnCxGroups             user's groups
     * @param pnCxRole               user's role
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, AddressVerificationDto addressVerificationDto,
                                                          CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole, List<TransactDeleteItemEnhancedRequest> deleteItemResponses) {

        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto, deleteItemResponses));
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo CORTESIA
     *
     * @param recipientId            id utente
     * @param senderId               eventuale id PA
     * @param courtesyChannelType    tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto) {
        return saveAddressBook(recipientId, senderId, null, courtesyChannelType, addressVerificationDto, null);
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo CORTESIA. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId            id utente
     * @param senderId               eventuale id PA
     * @param courtesyChannelType    tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @param pnCxType               user's type
     * @param pnCxRole               user's role
     * @param pnCxGroups             user's groups
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto,
                                                             CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto));
    }

    /**
     * Elimina un indirizzo di tipo LEGALE
     *
     * @param recipientId      id utente
     * @param senderId         id mittente
     * @param legalChannelType tipo canale legale
     * @return nd
     */
    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType) {
        return deleteAddressBook(recipientId, senderId, legalChannelType, null, false);
    }

    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, boolean transactional) {
        return deleteAddressBook(recipientId, senderId, legalChannelType, null, transactional);
    }

    /**
     * Elimina un indirizzo di tipo LEGALE. Per le persone giuridiche è prevista una validazione di accesso.
     *
     * @param recipientId      id utente
     * @param senderId         id mittente
     * @param legalChannelType tipo canale legale
     * @param pnCxType         user's type
     * @param pnCxRole         user's role
     * @param pnCxGroups       user's groups
     * @return nd
     */
    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType,
                                               CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> deleteLegalAddressBook(recipientId, senderId, legalChannelType));
    }

    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType,
                                               CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole, boolean transactional) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> deleteLegalAddressBook(recipientId, senderId, legalChannelType, transactional));
    }

    /**
     * Elimina un indirizzo di tipo CORTESIA
     *
     * @param recipientId         id utente
     * @param senderId            id mittente
     * @param courtesyChannelType tipo canale cortesia
     * @return nd
     */
    public Mono<Object> deleteCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType) {
        return deleteAddressBook(recipientId, senderId, null, courtesyChannelType, false);
    }

    /**
     * Elimina un indirizzo di tipo CORTESIA. Per le persone giuridiche è prevista una validazione di accesso.
     *
     * @param recipientId         id utente
     * @param senderId            id mittente
     * @param courtesyChannelType tipo canale cortesia
     * @param pnCxType            user's type
     * @param pnCxGroups          user's groups
     * @param pnCxRole            user's role
     * @return nd
     */
    public Mono<Object> deleteCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType,
                                                  CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> deleteCourtesyAddressBook(recipientId, senderId, courtesyChannelType));
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base a recipient e sender id
     *
     * @param recipientId id utente
     * @param senderId    id mittente
     * @return lista indirizzi di cortesia
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipientAndSender(String recipientId, String senderId) {

        return getAddressList(recipientId, senderId, CourtesyAddressTypeDto.COURTESY.getValue())
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> appIOUtils.enrichWithAppIo(recipientId, list))
                .flatMapIterable(x -> x);
    }

    @NotNull
    private Mono<List<AddressBookEntity>> getAddressList(String recipientId, String senderId, String type) {
        Tuple2<Mono<String>, Boolean> tuple = resolveSenderId(senderId);

        return tuple.mapT1(newSenderId ->
                newSenderId.flatMapMany(rootSenderId ->
                                dao.getAddresses(recipientId, rootSenderId, type, !tuple.getT2()).switchIfEmpty(
                                        tuple.getT2() ?
                                                getRoot(senderId).flatMapMany(retryId -> dao.getAddresses(recipientId, retryId, type, true))
                                                : Flux.empty()
                                ))
                        .collectList()).getT1();
    }

    //Tiny call
    private Mono<String> getRoot(String senderId) {
        return externalRegistryClient.getRootSenderId(senderId);
    }

    private Tuple2<Mono<String>, Boolean> resolveSenderId(String origSenderId) {
        Mono<String> sender;
        Boolean isSpecialSender;
        if (AddressBookEntity.SENDER_ID_DEFAULT.equals(origSenderId)) {
            sender = Mono.just(origSenderId);
            isSpecialSender = false;
        } else if (pnUserattributesConfig.getAoouosenderid().contains(origSenderId)) {
            sender = Mono.just(origSenderId);
            isSpecialSender = true;
        } else {
            sender = getRoot(origSenderId);
            isSpecialSender = false;
        }
        return Tuples.of(sender, isSpecialSender);
    }


    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId
     * Ritorna anche gli indirizzi in corso di validazione
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    private Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, CourtesyAddressTypeDto.COURTESY.getValue())
                .collectList()
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> appIOUtils.enrichWithAppIo(recipientId, list))
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId id utente
     * @param pnCxType    user's type
     * @param pnCxGroups  user's groups
     * @param pnCxRole    user's role
     * @return lista indirizzi
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId, CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMapMany(r -> getCourtesyAddressByRecipient(recipientId));
    }

    public Mono<Boolean> isAppIoEnabledByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, CourtesyAddressTypeDto.COURTESY.getValue())
                .filter(x -> x.getChannelType().equals(CourtesyChannelTypeDto.APPIO.getValue()))
                .take(1).next()
                .map(x -> true)
                .defaultIfEmpty(false);
    }

    /**
     * Ritorna gli indirizzi LEGALI per il recipient e il sender id
     *
     * @param recipientId id utente
     * @param senderId    id mittente
     * @return lista indirizzi
     */
    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipientAndSender(String recipientId, String senderId) {
        return getAddressList(recipientId, senderId, LegalAddressTypeDto.LEGAL.getValue())
                .flatMap(list -> deanonimizeLegal(recipientId, list))
                .flatMapIterable(x -> x);
    }


    /**
     * Lista indirizzi in base al recipient
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    private Flux<LegalAndUnverifiedDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, LegalAddressTypeDto.LEGAL.getValue())
                .collectList()
                .flatMap(list -> deanonimizeLegal(recipientId, list))
                .map(list -> list.stream().map(LegalDigitalAddressDtoToLegalAndUnverifiedDigitalAddressDtoMapper::toDto).toList())
                .zipWith(dao.getAllVerificationCodesByRecipient(recipientId, LegalAddressTypeDto.LEGAL.getValue())
                        .map(VerificationCodeEntityToLegalAndUnverifiedDigitalAddressDtoMapper::toDto)
                        .collectList())
                .map(tuple2 -> {
                    List<LegalAndUnverifiedDigitalAddressDto> res = new ArrayList<>();
                    res.addAll(tuple2.getT1());
                    res.addAll(tuple2.getT2());
                    return res;
                })
                .flatMapIterable(x -> x);
    }

    /**
     * Lista indirizzi in base al recipient. Per le persone giuridiche è prevista una validazione di accesso.
     *
     * @param recipientId id utente
     * @param pnCxType    user's type
     * @param pnCxRole    user's role
     * @param pnCxGroups  user's groups
     * @return lista indirizzi
     */
    public Flux<LegalAndUnverifiedDigitalAddressDto> getLegalAddressByRecipient(String recipientId, CxTypeAuthFleetDto pnCxType,
                                                                                List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMapMany(r -> getLegalAddressByRecipient(recipientId));
    }

    /**
     * Ritorna gli indirizzi LEGALI e di CORTESIA per il recipient. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId id utente
     * @param pnCxType    user's type
     * @param pnCxRole    user's role
     * @param pnCxGroups  user's groups
     * @return oggetto contenente le liste LEGALI e di CORTESIA
     */
    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId, CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> getAddressesByRecipient(recipientId));
    }

    /**
     * Ritorna gli indirizzi LEGALI e di CORTESIA per il recipient
     *
     * @param recipientId id utente
     * @return oggetto contenente le liste LEGALI e di CORTESIA
     */
    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        UserAddressesDto dto = new UserAddressesDto();
        dto.setCourtesy(new ArrayList<>());
        dto.setLegal(new ArrayList<>());


        return getCourtesyAddressByRecipient(recipientId).collectList().defaultIfEmpty(new ArrayList<>())
                .zipWith(getLegalAddressByRecipient(recipientId).collectList().defaultIfEmpty(new ArrayList<>()))
                .map(tuple -> {
                    dto.setCourtesy(tuple.getT1());
                    dto.setLegal(tuple.getT2());
                    return dto;
                })
                .flatMap(this::enrichWithPaNames);

    }

    private Mono<UserAddressesDto> enrichWithPaNames(UserAddressesDto dtoWithAddresses) {
        // per tutti quegli indirizzi che non hanno senderId = default, ricavo i nomi degli enti
        List<String> paIds1 = dtoWithAddresses.getCourtesy().stream().map(CourtesyDigitalAddressDto::getSenderId).filter(ids -> !ids.equals(AddressBookEntity.SENDER_ID_DEFAULT)).toList();
        List<String> paIds2 = dtoWithAddresses.getLegal().stream().map(LegalAndUnverifiedDigitalAddressDto::getSenderId).filter(ids -> !ids.equals(AddressBookEntity.SENDER_ID_DEFAULT)).toList();
        List<String> paIds = Stream.concat(paIds1.stream(), paIds2.stream())
                .distinct()
                .toList();

        if (paIds.isEmpty()) {
            return Mono.just(dtoWithAddresses);
        } else
            return pnSelfcareClient.getManyPaByIds(paIds).collectMap(PaSummary::getId, PaSummary::getName)
                    .map(paNames -> {
                        dtoWithAddresses.getCourtesy().forEach(x -> x.setSenderName(paNames.getOrDefault(x.getSenderId(), null)));
                        dtoWithAddresses.getLegal().forEach(x -> x.setSenderName(paNames.getOrDefault(x.getSenderId(), null)));
                        return dtoWithAddresses;
                    });
    }

    /**
     * Il metodo si occupa di salvare un indirizzo, gestendo di fatto le varie casisitiche
     * Il metodo prevede:
     * - Cercare in DB se esiste già un indirizzo verificato (senza considerare il channelType che può essere diverso), con la stessa SHA
     * - Se lo trova, salta la parte di invio codice e invoca il datavault per anonimizzarlo e salvare il valore anonimizzato in DB
     * - Se non lo trova, ho due casi possibili:
     * -- CASO A: non mi viene passato un codice verifica: è il caso più comune in cui l'utente crea un nuovo indirizzo per la prima volta.
     * - Salvo un nuovo VerificationCode e procedo all'invio del codice di verifica.
     * -- CASO B: mi viene passato un codice di verifica (valido): è il secondo step della procedura, in cui l'utente convalida l'indirizzo.
     * - Recupero il VerificationCode, e lo valido.
     * - Se valido, invoco il datavault per anonimizzarlo e salvare il valore anonimizzato in DB.
     *
     * @param recipientId            id utente
     * @param legalChannelType       tipologia canale legale
     * @param courtesyChannelType    tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> saveAddressBook(String recipientId, String firstSenderId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto, List<TransactDeleteItemEnhancedRequest> deleteItemResponses) {
        return filterNotRootSender(firstSenderId).flatMap(checkedSenderId ->
                {
                    String legal = verificationCodeUtils.getLegalType(legalChannelType);
                    String channelType = verificationCodeUtils.getChannelType(legalChannelType, courtesyChannelType);

                    if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
                        // le richieste da APPIO non hanno "indirizzo", posso procedere con l salvataggio in dynamodb,
                        // senza dover passare per la creazione di un VC
                        // Devo cmq creare un VA con il channelType
                        // l'auditlog viene fatto qui, in modo da racchiudere TUTTE le attività fatte (salvataggio su dynamo, invocazione BE io, ecc ecc)
                        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
                        String logMessage = String.format("saveAddressBook - enabling PN for IO - recipientId=%s - senderId=%s - channelType=%s", recipientId, null, CourtesyChannelTypeDto.APPIO);
                        PnAuditLogEvent logEvent = auditLogBuilder
                                .before(PnAuditLogEventType.AUD_AB_DA_IO_INSUP, logMessage)
                                .build();
                        logEvent.log();

                        return appIOUtils.sendToIoActivationServiceAndSaveInDynamodb(recipientId, legal, checkedSenderId, channelType)
                                .onErrorResume(throwable -> {
                                    logEvent.generateFailure("failed saving exception={}", throwable.getMessage(), throwable).log();
                                    return Mono.error(throwable);
                                })
                                .map(m -> {
                                    log.info("setCourtesyAddressIo done - recipientId={} - senderId={} - channelType={} res={}", recipientId, null, CourtesyChannelTypeDto.APPIO, m);
                                    logEvent.generateSuccess(logMessage).log();
                                    return m;
                                })
                                .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
                    } else {
                        return verificationCodeUtils.validateHashedAddress(recipientId, legalChannelType, courtesyChannelType, addressVerificationDto)
                                .flatMap(res -> {
                                    if (Boolean.TRUE.equals(res)) {
                                        // l'indirizzo risulta già verificato precedentemente, posso procedere con il salvataggio in data-vault,
                                        // senza dover passare per la creazione di un VC
                                        // Devo cmq creare un VA con il channelType
                                        // creo un record fittizio di verificationCode, così evito di passare tutti i parametri
                                        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashAddress(addressVerificationDto.getValue()),
                                                channelType, checkedSenderId, legal, addressVerificationDto.getValue());
                                        return verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCode, deleteItemResponses);
                                    } else {
                                        // l'indirizzo non è verificato. Ho due casi possibili:
                                        if (!StringUtils.hasText(addressVerificationDto.getVerificationCode())) {
                                            // CASO A: non mi viene passato un codice verifica
                                            return verificationCodeUtils.saveInDynamodbNewVerificationCodeAndSendToExternalChannel(
                                                    recipientId,
                                                    addressVerificationDto.getValue(),
                                                    legalChannelType,
                                                    courtesyChannelType,
                                                    checkedSenderId);
                                        } else {
                                            // CASO B: ho un codice di verifica da validare e poi procedere.
                                            return verificationCodeUtils.validateVerificationCodeAndSendToDataVault(recipientId, addressVerificationDto, legalChannelType, courtesyChannelType);
                                        }

                                    }
                                });
                    }
                })
                .switchIfEmpty(
                        Mono.error(new PnAddressNotFoundException())
                );

    }

    private Mono<String> filterNotRootSender(final String senderId) {

        if (senderId == null) return Mono.just(AddressBookEntity.SENDER_ID_DEFAULT);

        return externalRegistryClient.getAooUoIdsApi(Arrays.asList(senderId)).next()
                .switchIfEmpty(Mono.just("")) //Root case
                .flatMap(s -> {
                            if (StringUtils.hasText(s)) {
                                // Not Root
                                return Mono.error(new PnInvalidInputException(ERROR_CODE_USERATTRIBUTES_SENDERIDNOTROOT, "sender Id not root, cannot save address"));
                            } else {
                                // Root
                                return Mono.just(senderId);
                            }
                        }
                );

    }


    /**
     * Elimina un indirizzo
     *
     * @param recipientId         id utente
     * @param senderId            id mittente
     * @param legalChannelType    eventuale canale legale
     * @param courtesyChannelType eventuale canale cortesia
     * @return nd
     */
    private Mono<Object> deleteAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, boolean transactional) {
        log.info("deleteAddressBook recipientId={} senderId={} legalChannelType={} courtesyChannelType={}", recipientId, senderId, legalChannelType, courtesyChannelType);
        String legal = verificationCodeUtils.getLegalType(legalChannelType);
        String channelType = verificationCodeUtils.getChannelType(legalChannelType, courtesyChannelType);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);

        if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
            // le richieste da APPIO hanno una gestione complessa dedicata
            // l'auditlog viene fatto qui, in modo da racchiudere TUTTE le attività fatte (salvataggio su dynamo, invocazione BE io, ecc ecc)
            String logMessage = String.format("deleteAddressBook  - disabling PN for IO - recipientId=%s - senderId=%s - channelType=%s", recipientId, null, CourtesyChannelTypeDto.APPIO);
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_AB_DA_IO_DEL, logMessage)
                    .build();
            logEvent.log();
            return appIOUtils.deleteAddressBookAppIo(addressBookEntity)
                    .onErrorResume(throwable -> {
                        logEvent.generateFailure("failed saving exception={}", throwable.getMessage(), throwable).log();
                        return Mono.error(throwable);
                    })
                    .map(m -> {
                        log.info("deleteAddressBook done - recipientId={} - senderId={} - channelType={} res={}", recipientId, null, CourtesyChannelTypeDto.APPIO, m);
                        logEvent.generateSuccess(logMessage).log();
                        return m;
                    });
        } else {
            return dataVaultClient.deleteRecipientAddressByInternalId(recipientId, addressBookEntity.getAddressId())
                    .then(dao.deleteAddressBook(recipientId, senderId, legal, channelType, transactional));
        }
    }


    private Mono<List<CourtesyDigitalAddressDto>> deanonimizeCourtesy(String recipientId, List<AddressBookEntity> list) {
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


    private Mono<List<LegalDigitalAddressDto>> deanonimizeLegal(String recipientId, List<AddressBookEntity> list) {
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

    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> manageError(Optional<PnAuditLogEvent> optionalPnAuditLogEvent, Throwable throwable) {
        if (throwable instanceof PnInvalidVerificationCodeException ||
                throwable instanceof PnExpiredVerificationCodeException ||
                throwable instanceof PnRetryLimitVerificationCodeException) {
            optionalPnAuditLogEvent.ifPresent(pnAuditLogEvent ->
                    pnAuditLogEvent.generateWarning("codice non valido - {}", throwable.getMessage()).log());
        } else {
            optionalPnAuditLogEvent.ifPresent(pnAuditLogEvent ->
                    pnAuditLogEvent.generateFailure(throwable.getMessage()).log());
        }
        return Mono.error(throwable);
    }

}


