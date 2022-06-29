package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.iofunctionservices.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.iofunctionservices.v1.api.DefaultApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.iofunctionservices.v1.dto.Activation;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.iofunctionservices.v1.dto.ActivationPayload;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.iofunctionservices.v1.dto.FiscalCodePayload;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.OcpBaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

/**
 * Classe wrapper di io-function-services, con gestione del backoff
 */
@Component
@Slf4j
public class IoFunctionServicesClient extends OcpBaseClient {

    public static final String IO_STATUS_ACTIVE = "ACTIVE";
    public static final String IO_STATUS_INACTIVE = "INACTIVE";
    private DefaultApi ioApi;
    private final PnUserattributesConfig pnUserattributesConfig;
    private final PnDataVaultClient pnDataVaultClient;

    public IoFunctionServicesClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient pnDataVaultClient) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.pnDataVaultClient = pnDataVaultClient;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder(), pnUserattributesConfig.getClientIoactivationservicesApikey()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientIoactivationservicesBasepath());

        this.ioApi = new DefaultApi(apiClient);
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
        log.info("upsertServiceActivation internalId:{} activated:{}", internalId, activated);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    ActivationPayload dto = new ActivationPayload();
                    dto.setFiscalCode(user.getTaxId());
                    dto.setStatus(activated? IO_STATUS_ACTIVE : IO_STATUS_INACTIVE);

                    return ioApi.upsertServiceActivation(dto)
                            .retryWhen(
                                    Retry.backoff(2, Duration.ofMillis(25))
                                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                            )
                            .onErrorResume(throwable -> {
                                log.error("error upserting service activation", throwable);
                                return getServiceActivation(internalId);
                            })
                            .map(x -> {
                                log.info("upsertServiceActivation response taxid={} status={} serviceId={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getServiceId(), x.getVersion());
                                return x.getStatus().equals(IO_STATUS_ACTIVE);
                            });
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
        log.info("getServiceActivation internalId:{}", internalId);

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
                                log.info("getServiceActivation response taxid={} status={} serviceId={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getServiceId(), x.getVersion());
                                return x;
                            });
                });
    }

}
