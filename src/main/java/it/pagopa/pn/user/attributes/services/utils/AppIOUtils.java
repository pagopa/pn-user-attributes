package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyAddressTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.UserStatusResponse;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppIOUtils {

    private final AddressBookDao dao;
    private final VerifiedAddressUtils verifiedAddressUtils;
    private final PnExternalRegistryIoClient pnExternalRegistryClient;
    private final IONotificationService ioNotificationService;

    private static final String PF_PREFIX = "PF-";

    @NotNull
    public Mono<Object> deleteAddressBookAppIo(AddressBookEntity addressBookEntity) {
        // le richieste da APPIO non hanno "indirizzo", posso procedere con l'eliminazione in dynamodb, che però è solo logica, quindi vado a impostare il flag a FALSE
        AtomicBoolean waspresent = new AtomicBoolean(true);
        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_DISABLED);

        return dao.getAddressBook(addressBookEntity)
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.info("Never activated, proceeding with io-deactivation");
                    waspresent.set(false);
                    return addressBookEntity;
                }))
                .then(verifiedAddressUtils.saveInDynamodb(addressBookEntity))
                .then(this.pnExternalRegistryClient.upsertServiceActivation(addressBookEntity.getRecipientId(), false))
                .onErrorResume(throwable -> {
                    if (waspresent.get()) {
                        log.error("Saving to io-activation-service failed, re-adding to addressbook appio channeltype");
                        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_ENABLED);
                        return verifiedAddressUtils.saveInDynamodb(addressBookEntity)
                                .then(Mono.error(throwable));
                    } else
                        return Mono.error(throwable);
                })
                .flatMap(activated -> {
                    if (Boolean.TRUE.equals(activated)) {
                        log.error("outcome io-status is activated, re-adding to addressbook appio channeltype");
                        addressBookEntity.setAddresshash(AddressBookEntity.APP_IO_ENABLED);
                        return verifiedAddressUtils.saveInDynamodb(addressBookEntity)
                                .then(Mono.error(new PnInternalException("IO deactivation failed", ERROR_CODE_IO_DEACTIVATION_FAILED)));
                    } else {
                        log.info("outcome io-status is not activated, deletion successful");
                        return Mono.just(new Object());
                    }
                })
                .then(Mono.just(new Object()));
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
    public Mono<AddressBookService.SAVE_ADDRESS_RESULT> sendToIoActivationServiceAndSaveInDynamodb(String recipientId, String legal, String senderId, String channelType)
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
                        return verifiedAddressUtils.saveInDynamodb(addressBookEntity)
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
                .then(Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS));
    }


    public Mono<List<CourtesyDigitalAddressDto>> enrichWithAppIo(String recipientId, List<CourtesyDigitalAddressDto> source)
    {
        log.info("enrichWithAppIo recipientId={}", recipientId);
        // devo controllare che l'APPIO non sia presente tra i risultati.
        // se è presente, è perchè è abilitata, e quindi non serve fare altro.
        // altrimenti, devo chiedere al BE di IO se l'utente è un utente di APPIO o no
        Optional<CourtesyDigitalAddressDto> appioAddress = source.stream().filter(x -> x.getChannelType().getValue().equals(CourtesyChannelTypeDto.APPIO.getValue())).findFirst();
        if (appioAddress.isEmpty())
        {
            // se l'utente è di tipo PF allora è prevista la possibilità di avere l'app IO, altrimenti non c'è sicuramente
            if (isPFInternalId(recipientId)) {
                // mi ricavo il CF da datavault, poi lo uso per recuperare se è un utente valido
                return this.pnExternalRegistryClient.checkValidUsers(recipientId)
                        .map(user -> {
                            // se non è attivo su IO, ritorno il dto SENZA APPIO
                            if (user.getStatus() == UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE) {
                                log.info("enrichWithAppIo appio is not available, not returning appio courtesy recipientId={}", recipientId);
                                return source;
                            } else if (user.getStatus() == UserStatusResponse.StatusEnum.ERROR) {
                                throw new PnInternalException("IO user check status failed", ERROR_CODE_IO_ERROR);
                            } else {
                                log.info("enrichWithAppIo appio is available, adding appio courtesy as disabled recipientId={}", recipientId);
                                // altrimenti, vuol dire che è presente ma disabilitato.
                                // si noti infatti che NON posso fidarmi del mio flag di disabilitato,
                                // perchè quel flag "DISABLED" da noi in PN può rappresentare sia il "APPIO non ATTIVO", sia "APPIO attivo ma PN disablitato"
                                CourtesyDigitalAddressDto add = new CourtesyDigitalAddressDto();
                                add.setValue(AddressBookEntity.APP_IO_DISABLED);
                                add.setRecipientId(recipientId);
                                add.setChannelType(CourtesyChannelTypeDto.APPIO);
                                add.setAddressType(CourtesyAddressTypeDto.COURTESY);
                                add.setSenderId(AddressBookEntity.SENDER_ID_DEFAULT);
                                source.add(add);
                                return source;
                            }
                        });
            }
            else
            {
                log.info("enrichWithAppIo appio courtesy is not available for PG recipientId={}", recipientId);
                return Mono.just(source);
            }
        }
        else
        {
            log.info("enrichWithAppIo appio courtesy is already enabled recipientId={}", recipientId);
            return Mono.just(source);
        }
    }

    private boolean isPFInternalId(String internalId)
    {
        return internalId != null && internalId.startsWith(PF_PREFIX);
    }
}
