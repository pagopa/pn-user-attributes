package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.v1.api.PrivacyNoticeApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.v1.dto.PrivacyNoticeVersionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@Slf4j
public class PnExternalRegistryClient extends CommonBaseClient {

    private PrivacyNoticeApi extregistryApi;
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnExternalRegistryClient(PnUserattributesConfig pnUserattributesConfig ) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        this.extregistryApi = new PrivacyNoticeApi(apiClient);
    }

    /**
     * Recupera la versione per la privacy corrente
     *
     * @param consentType tipo consenso
     * @param portalType portale
     *
     * @return void
     */
    public Mono<String> findPrivacyNoticeVersion(String consentType, String portalType)
    {
        log.info("findPrivacyNoticeVersion consentType={} portalType={}", consentType, portalType);

        return this.extregistryApi.findPrivacyNoticeVersion(consentType, portalType)
                .map(PrivacyNoticeVersionResponse::getVersion)
                .map(Object::toString);
    }

}
