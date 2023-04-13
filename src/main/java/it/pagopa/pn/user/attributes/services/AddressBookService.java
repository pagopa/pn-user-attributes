package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.*;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.selfcare.v1.dto.PaSummary;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnSelfcareClient;
import it.pagopa.pn.user.attributes.services.utils.AppIOUtils;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.utils.PgUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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



    public enum SAVE_ADDRESS_RESULT{
        SUCCESS,
        CODE_VERIFICATION_REQUIRED,
        PEC_VALIDATION_REQUIRED
    }


    public AddressBookService(AddressBookDao dao,
                              PnDataVaultClient dataVaultClient,
                              AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto,
                              PnSelfcareClient pnSelfcareClient, VerificationCodeUtils verificationCodeUtils, AppIOUtils appIOUtils) {
        this.dao = dao;
        this.dataVaultClient = dataVaultClient;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
        this.pnSelfcareClient = pnSelfcareClient;
        this.verificationCodeUtils = verificationCodeUtils;
        this.appIOUtils = appIOUtils;
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
    public Mono<SAVE_ADDRESS_RESULT> saveLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, AddressVerificationDto addressVerificationDto) {
        return saveAddressBook(recipientId, senderId, legalChannelType, null,  addressVerificationDto);
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo LEGALE. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param legalChannelType tipologia canale legale
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @param pnCxType user's type
     * @param pnCxGroups user's groups
     * @param pnCxRole user's role
     * @return risultato operazione
     */
    public Mono<SAVE_ADDRESS_RESULT> saveLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, AddressVerificationDto addressVerificationDto,
                                                          CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto));
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
    public Mono<SAVE_ADDRESS_RESULT> saveCourtesyAddressBook(String recipientId, String senderId, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto) {
        return saveAddressBook(recipientId, senderId, null, courtesyChannelType,  addressVerificationDto);
    }

    /**
     * Il metodo si occupa di salvare un indirizzo di tipo CORTESIA. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param courtesyChannelType tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @param pnCxType user's type
     * @param pnCxRole user's role
     * @param pnCxGroups user's groups
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
     * @param recipientId id utente
     * @param senderId id mittente
     * @param legalChannelType tipo canale legale
     * @return nd
     */
    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType) {
        return deleteAddressBook(recipientId, senderId, legalChannelType, null);
    }

    /**
     * Elimina un indirizzo di tipo LEGALE. Per le persone giuridiche è prevista una validazione di accesso.
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @param legalChannelType tipo canale legale
     * @param pnCxType user's type
     * @param pnCxRole user's role
     * @param pnCxGroups user's groups
     * @return nd
     */
    public Mono<Object> deleteLegalAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType,
                                               CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
        return PgUtils.validaAccesso(pnCxType, pnCxRole, pnCxGroups)
                .flatMap(r -> deleteLegalAddressBook(recipientId, senderId, legalChannelType));
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
     * Elimina un indirizzo di tipo CORTESIA. Per le persone giuridiche è prevista una validazione di accesso.
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @param courtesyChannelType tipo canale cortesia
     * @param pnCxType user's type
     * @param pnCxGroups user's groups
     * @param pnCxRole user's role
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
     * @param senderId id mittente
     * @return lista indirizzi di cortesia
     */
    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipientAndSender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, CourtesyAddressTypeDto.COURTESY.getValue())
                .collectList()
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> appIOUtils.enrichWithAppIo(recipientId, list))
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId
     * Ritorna anche gli indirizzi in corso di validazione
     *
     * @param recipientId id utente
     * @return lista indirizzi
     */
    public Flux<CourtesyAndUnverifiedDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId, CourtesyAddressTypeDto.COURTESY.getValue())
                .collectList()
                .flatMap(list -> deanonimizeCourtesy(recipientId, list))
                .flatMap(list -> appIOUtils.enrichWithAppIo(recipientId, list))
                .map(list -> list.stream().map(CourtesyDigitalAddressDtoToCourtesyAndUnverifiedDigitalAddressDtoMapper::toDto).toList())
                .zipWith(dao.getAllVerificationCodesByRecipient(recipientId, CourtesyAddressTypeDto.COURTESY.getValue())
                        .map(VerificationCodeEntityToCourtesyAndUnverifiedDigitalAddressDtoMapper::toDto)
                        .collectList())
                .map(tuple2 -> {
                    List<CourtesyAndUnverifiedDigitalAddressDto> res = new ArrayList<>();
                    res.addAll(tuple2.getT1());
                    res.addAll(tuple2.getT2());
                    return res;
                })
                .flatMapIterable(x -> x);
    }

    /**
     * Ritorna gli indirizzi di CORTESIA in base al recipientId. Per le persone giuridiche è prevista una validazione
     * di accesso.
     *
     * @param recipientId id utente
     * @param pnCxType user's type
     * @param pnCxGroups user's groups
     * @param pnCxRole user's role
     * @return lista indirizzi
     */
    public Flux<CourtesyAndUnverifiedDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId, CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole) {
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
     * Ritorna gli indirizzi LEGALI per li recipitent e il sender id
     *
     * @param recipientId id utente
     * @param senderId id mittente
     * @return lista indirizzi
     */
    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipientAndSender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId,  LegalAddressTypeDto.LEGAL.getValue())
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
    public Flux<LegalAndUnverifiedDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
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
     * @param pnCxType user's type
     * @param pnCxRole user's role
     * @param pnCxGroups user's groups
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
     * @param pnCxType user's type
     * @param pnCxRole user's role
     * @param pnCxGroups user's groups
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
            List<String> paIds1 = dtoWithAddresses.getCourtesy().stream().map(CourtesyAndUnverifiedDigitalAddressDto::getSenderId).filter(ids -> !ids.equals(AddressBookEntity.SENDER_ID_DEFAULT)).toList();
            List<String> paIds2 = dtoWithAddresses.getLegal().stream().map(LegalAndUnverifiedDigitalAddressDto::getSenderId).filter(ids -> !ids.equals(AddressBookEntity.SENDER_ID_DEFAULT)).toList();
            List<String> paIds = Stream.concat(paIds1.stream(),paIds2.stream())
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
     *    - Salvo un nuovo VerificationCode e procedo all'invio del codice di verifica.
     * -- CASO B: mi viene passato un codice di verifica (valido): è il secondo step della procedura, in cui l'utente convalida l'indirizzo.
     *    - Recupero il VerificationCode, e lo valido.
     *    - Se valido, invoco il datavault per anonimizzarlo e salvare il valore anonimizzato in DB.
     *
     * @param recipientId id utente
     * @param senderId eventuale id PA
     * @param legalChannelType tipologia canale legale
     * @param courtesyChannelType tipologia canale cortesia
     * @param addressVerificationDto dto con indirizzo e codice verifica
     * @return risultato operazione
     */
    private Mono<SAVE_ADDRESS_RESULT> saveAddressBook(String recipientId, String senderId, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, AddressVerificationDto addressVerificationDto) {
        String legal = verificationCodeUtils.getLegalType(legalChannelType);
        String channelType = verificationCodeUtils.getChannelType(legalChannelType, courtesyChannelType);

        if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
            // le richieste da APPIO non hanno "indirizzo", posso procedere con l salvataggio in dynamodb,
            // senza dover passare per la creazione di un VC
            // Devo cmq creare un VA con il channelType
            return appIOUtils.sendToIoActivationServiceAndSaveInDynamodb(recipientId, legal, senderId, channelType)
                    .then(Mono.just(SAVE_ADDRESS_RESULT.SUCCESS));
        }
        else {

            verificationCodeUtils.validateAddress(legalChannelType, courtesyChannelType, addressVerificationDto);
            String hashedAddress = verificationCodeUtils.hashAddress(addressVerificationDto.getValue());

            return  dao.validateHashedAddress(recipientId, hashedAddress, channelType)
                    .flatMap(res -> {
                        if (res == AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED) {
                            // l'indirizzo risulta già verificato precedentemente, posso procedere con il salvataggio in data-vault,
                            // senza dover passare per la creazione di un VC
                            // Devo cmq creare un VA con il channelType
                            // creo un record fittizio di verificationCode, così evito di passare tutti i parametri
                            VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, hashedAddress, channelType, senderId, legal, addressVerificationDto.getValue());
                            return verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCode, addressVerificationDto.getValue());
                        } else {
                            // l'indirizzo non è verificato. Ho due casi possibili:
                            if (!StringUtils.hasText(addressVerificationDto.getVerificationCode())) {
                                // CASO A: non mi viene passato un codice verifica
                                return verificationCodeUtils.saveInDynamodbNewVerificationCodeAndSendToExternalChannel(recipientId, addressVerificationDto.getValue(), legalChannelType, courtesyChannelType, senderId);
                            } else {
                                // CASO B: ho un codice di verifica da validare e poi procedere.
                                return verificationCodeUtils.validateVerificationCodeAndSendToDataVault(recipientId, addressVerificationDto, legalChannelType, courtesyChannelType);
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
        String legal = verificationCodeUtils.getLegalType(legalChannelType);
        String channelType = verificationCodeUtils.getChannelType(legalChannelType, courtesyChannelType);
        AddressBookEntity addressBookEntity = new AddressBookEntity(recipientId, legal, senderId, channelType);

        if (courtesyChannelType != null && courtesyChannelType.equals(CourtesyChannelTypeDto.APPIO)) {
            // le richieste da APPIO hanno una gestione complessa dedicata
            return appIOUtils.deleteAddressBookAppIo(addressBookEntity);
        }
        else {
            return dataVaultClient.deleteRecipientAddressByInternalId(recipientId, addressBookEntity.getAddressId())
                    .then(dao.deleteAddressBook(recipientId, senderId, legal, channelType));
        }
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
