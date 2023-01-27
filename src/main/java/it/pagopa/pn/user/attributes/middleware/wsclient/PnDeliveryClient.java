package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.api.InternalOnlyApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.NotificationStatus;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@Slf4j
public class PnDeliveryClient extends CommonBaseClient {

    private InternalOnlyApi pnDeliveryApi;
    private final PnUserattributesConfig pnUserattributesConfig;


    public PnDeliveryClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDeliveryBasepath());

        this.pnDeliveryApi = new InternalOnlyApi(apiClient);

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
    public Flux<SentNotification> searchNotificationPrivate(OffsetDateTime startDate, OffsetDateTime endDate, String internalId)
    {
        log.info("searchNotificationPrivate internalId={} startDate={} endDate={}", internalId, startDate, endDate);

        return this.pnDeliveryApi.searchNotificationsPrivate(startDate, endDate, internalId, true, null,
                        List.of(NotificationStatus.ACCEPTED, NotificationStatus.DELIVERING, NotificationStatus.DELIVERED), 100, null)
                .onErrorResume(throwable -> {
                    log.error("error searchNotificationsPrivate message={}", elabExceptionMessage(throwable) , throwable);
                    return Mono.error(throwable);
                })
                .map(res -> {
                    log.info("search result internalId={} returned size={}", internalId, res.getResultsPage().size());
                    return res.getResultsPage();
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(notification -> this.pnDeliveryApi.getSentNotificationPrivate(notification.getIun()));
    }

    public Mono<SentNotification> getSentNotificationPrivate(String iun)
    {
        log.info("getSentNotificationPrivate iun={}", iun);

        return this.pnDeliveryApi.getSentNotificationPrivate(iun)
                .onErrorResume(throwable -> {
                    log.error("error getSentNotificationPrivate message={}", elabExceptionMessage(throwable) , throwable);
                    return Mono.error(throwable);
                });
    }

}
