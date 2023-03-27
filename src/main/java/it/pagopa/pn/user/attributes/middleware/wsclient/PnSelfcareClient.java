package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.selfcare.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.selfcare.v1.api.InfoPaApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.selfcare.v1.dto.PaSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe wrapper di self-care, con gestione del backoff
 */
@Component
@Slf4j
public class PnSelfcareClient extends CommonBaseClient {
    private InfoPaApi infoPaApi;
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnSelfcareClient(PnUserattributesConfig pnUserattributesConfig ) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());
        this.infoPaApi = new InfoPaApi(apiClient);
    }

    /**
     * Retrieve summary information about many PA
     *
     * @param ids pa ids list
     *
     * @return Flux<PaSummary>
     */
    public Mono<List<PaSummary>> getManyPaByIds(List<String> ids)
    {
        log.info("getManyPaByIds ids={}", ids);
        return this.infoPaApi.getManyPa(ids).collectList();
    }
}
