package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.api.PrivacyNoticeApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.dto.PrivacyNoticeVersionResponse;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.AooUoIdsApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.RootSenderIdApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.dto.RootSenderIdResponse;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnExternalRegistryClient {

    private final PrivacyNoticeApi extregistryApi;
    private final RootSenderIdApi rootSenderIdApi;

    private final AooUoIdsApi aooUoIdsApi;

    public PnExternalRegistryClient(PrivacyNoticeApi extregistryApi, RootSenderIdApi rootSenderIdApi
        ,AooUoIdsApi aooUoIdsApi) {
        this.extregistryApi = extregistryApi;
        this.rootSenderIdApi = rootSenderIdApi;
        this.aooUoIdsApi = aooUoIdsApi;
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
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Retrieving privacy notice version");
        log.debug("findPrivacyNoticeVersion consentType={} portalType={}", consentType, portalType);

        return this.extregistryApi.findPrivacyNoticeVersion(consentType, portalType)
                .map(PrivacyNoticeVersionResponse::getVersion)
                .map(Object::toString);
    }

    @Cacheable(value = "aooSenderIdCache")
    public Mono<String> getRootSenderId(String id){
        log.info("asking rootId for sender {}", id);
        return this.rootSenderIdApi.getRootSenderIdPrivate(id).map(RootSenderIdResponse::getRootId);
    }

    public Flux<String> getAooUoIdsApi (List<String> ids){
        log.info("filtering just aoo/uo ids {}", ids);
        return this.aooUoIdsApi.getFilteredAooUoIdPrivate(ids);
    }
}

