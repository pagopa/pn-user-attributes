package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.api.PrivacyNoticeApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MsClientExtRegistryConfig extends CommonBaseClient {

    @Value("${pn.commons.read-timeout-millis}") int origReadTimeoutMillis;

    @Bean
    PrivacyNoticeApi privacyNoticeApi(PnUserattributesConfig pnUserattributesConfig) {
        // imposto un timeout custom, per me
        setReadTimeoutMillis((int)Math.round(origReadTimeoutMillis * 1.5));

        WebClient webClient = initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.ApiClient.buildWebClientBuilder());

        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.ApiClient(webClient);
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        return new PrivacyNoticeApi(apiClient);
    }


}
