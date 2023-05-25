package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.api.PrivacyNoticeApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.dto.PrivacyNoticeVersionResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnExternalRegistryClient {

    private final PrivacyNoticeApi extregistryApi;

    public PnExternalRegistryClient(PrivacyNoticeApi extregistryApi) {
        this.extregistryApi = extregistryApi;
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

}
