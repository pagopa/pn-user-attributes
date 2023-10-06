package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.InfoPaApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.dto.PaSummary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Classe wrapper di self-care, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnSelfcareClient extends CommonBaseClient {
    private final InfoPaApi infoPaApi;

    public PnSelfcareClient(InfoPaApi infoPaApi) {
        this.infoPaApi = infoPaApi;
    }

    /**
     * Retrieve summary information about many PA
     *
     * @param ids pa ids list
     *
     * @return Flux<PaSummary>
     */
    public Flux<PaSummary> getManyPaByIds(List<String> ids)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.SELFCARE_PA, "Retrieving PAs summary infos");
        log.debug("getManyPaByIds ids={}", ids);
        return this.infoPaApi.getManyPa(ids).doOnError(
                throwable -> log.logInvokationResultDownstreamFailed(PnLogger.EXTERNAL_SERVICES.SELFCARE_PA, throwable.getMessage()));
    }
}
