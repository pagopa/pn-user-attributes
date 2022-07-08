package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.api.InternalOnlyApi;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@Slf4j
public class PnDeliveryClient extends BaseClient {

    private InternalOnlyApi pnDeliveryApi;
    private final PnUserattributesConfig pnUserattributesConfig;
    private final PnDataVaultClient pnDataVaultClient;
    private final PnAuditLogBuilder auditLogBuilder;

    public PnDeliveryClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient pnDataVaultClient, PnAuditLogBuilder pnAuditLogBuilder) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.pnDataVaultClient = pnDataVaultClient;
        this.auditLogBuilder = pnAuditLogBuilder;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientDeliveryBasepath());

        this.pnDeliveryApi = new InternalOnlyApi(apiClient);

    }

    /**
     * Crea (o aggiorna) lo stato in IO
     *
     * @param internalId internalId dell'utente
     *
     * @return void
     */
    public Mono<Boolean> searchNotificationPrivate(String internalId)
    {
        log.info("searchNotificationPrivate internalId={} activated={}", internalId, activated);

        return this.pnDeliveryApi.getSentNotificationPrivate().getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    ActivationPayload dto = new ActivationPayload();
                    dto.setFiscalCode(user.getTaxId());
                    dto.setStatus(activated? ActivationStatus.ACTIVE : ActivationStatus.INACTIVE);

                    return ioApi.upsertServiceActivation(dto)
                            .retryWhen(
                                    Retry.backoff(2, Duration.ofMillis(25))
                                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                            )
                            .onErrorResume(throwable -> {
                                log.error("error upserting service activation message={}", elabExceptionMessage(throwable) , throwable);
                                return getServiceActivation(internalId);
                            })
                            .map(x -> {
                                log.info("upsertServiceActivation response taxid={} status={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getVersion());
                                return x.getStatus().equals(ActivationStatus.ACTIVE);
                            });
                });
    }


}
