package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.NotificationStatus;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV21;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.commons.pnclients.CommonBaseClient.elabExceptionMessage;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnDeliveryClient {

    private final InternalOnlyApi pnDeliveryApi;


    public PnDeliveryClient(InternalOnlyApi pnDeliveryApi) {
        this.pnDeliveryApi = pnDeliveryApi;
    }


    /**
     * Ricerca le notifiche, risolve già le notifiche per intero
     *
     * @param startDate data inizio ricerca
     * @param endDate data fine ricerca
     * @param internalId internalId dell'utente
     *
     * @return void
     */
    public Flux<SentNotificationV21> searchNotificationPrivate(OffsetDateTime startDate, OffsetDateTime endDate, String internalId)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY, "Search notifications for user");
        log.debug("searchNotificationPrivate internalId={} startDate={} endDate={}", internalId, startDate, endDate);

        return this.pnDeliveryApi.searchNotificationsPrivate(startDate, endDate, internalId, true, null,
                        List.of(NotificationStatus.ACCEPTED, NotificationStatus.DELIVERING, NotificationStatus.DELIVERED), 50, null)
                .onErrorResume(throwable -> {
                    log.error("error searchNotificationsPrivate message={}", elabExceptionMessage(throwable) , throwable);
                    return Mono.error(throwable);
                })
                .map(res -> {
                    log.debug("search result internalId={} returned size={}", internalId, res.getResultsPage().size());
                    return res.getResultsPage();
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(notification -> {
                    log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY, "Retrieve notification");
                    log.debug("searchNotificationPrivate iun={}", notification.getIun());
                    return this.pnDeliveryApi.getSentNotificationPrivate(notification.getIun());
                });
    }

    public Mono<SentNotificationV21> getSentNotificationPrivate(String iun)
    {
        log.info("getSentNotificationPrivate iun={}", iun);

        return this.pnDeliveryApi.getSentNotificationPrivate(iun)
                .onErrorResume(throwable -> {
                    log.error("error getSentNotificationPrivate message={}", elabExceptionMessage(throwable) , throwable);
                    return Mono.error(throwable);
                });
    }

}
