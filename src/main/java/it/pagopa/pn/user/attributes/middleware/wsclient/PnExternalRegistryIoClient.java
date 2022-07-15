package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.api.IoActivationApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.api.SendIoMessageApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.*;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
public class PnExternalRegistryIoClient extends BaseClient {

    private IoActivationApi ioApi;
    private SendIoMessageApi ioMessageApi;
    private final PnUserattributesConfig pnUserattributesConfig;
    private final PnDataVaultClient pnDataVaultClient;
    private final PnAuditLogBuilder auditLogBuilder;

    public PnExternalRegistryIoClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient pnDataVaultClient, PnAuditLogBuilder pnAuditLogBuilder) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.pnDataVaultClient = pnDataVaultClient;
        this.auditLogBuilder = pnAuditLogBuilder;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        this.ioApi = new IoActivationApi(apiClient);

        apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        this.ioMessageApi = new SendIoMessageApi(apiClient);
    }

    /**
     * Crea (o aggiorna) lo stato in IO
     *
     * @param internalId internalId dell'utente
     * @param activated indica se attivato o disattivato
     *
     * @return void
     */
    public Mono<Boolean> upsertServiceActivation(String internalId, boolean activated)
    {
        log.info("upsertServiceActivation internalId={} activated={}", internalId, activated);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
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

    public Mono<UserStatusResponse> checkValidUsers(String internalId) throws WebClientResponseException {

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    UserStatusRequest userStatusRequest = new UserStatusRequest();
                    userStatusRequest.setTaxId(user.getTaxId());
                    return this.ioMessageApi.userStatus(userStatusRequest)
                            .retryWhen(
                                    Retry.backoff(2, Duration.ofMillis(25))
                                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                            );
                });
    }

    /**
     * Ritorna lo stato lo stato in IO
     *
     * @param internalId internalId dell'utente
     *
     * @return void
     */
    public Mono<Activation> getServiceActivation(String internalId)
    {
        log.info("getServiceActivation internalId={}", internalId);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    FiscalCodePayload dto = new FiscalCodePayload();
                    dto.setFiscalCode(user.getTaxId());

                    return ioApi.getServiceActivationByPOST(dto)
                            .retryWhen(
                                    Retry.backoff(2, Duration.ofMillis(25))
                                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                            )
                            .map(x -> {
                                log.info("getServiceActivation response taxid={} status={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getVersion());
                                return x;
                            });
                });
    }

    public Mono<SendMessageResponse> sendIOMessage(SendMessageRequest sendMessageRequest) {
        log.info("sendIOMessage sendMessageRequest iun={}", sendMessageRequest.getIun());

        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_AD_SEND_IO, "sendIOMessage")
                .iun(sendMessageRequest.getIun())
                .build();

        logEvent.log();
        return this.ioMessageApi.sendIOMessage(sendMessageRequest)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(res -> {
                    logEvent.generateSuccess().log();
                    return res;
                });
    }


}
