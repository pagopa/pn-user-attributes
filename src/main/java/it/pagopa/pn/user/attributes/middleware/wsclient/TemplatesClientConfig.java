package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.api.TemplateApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TemplatesClientConfig {

    @Bean
    @Primary
    public TemplateApi templateApiConfig(@Qualifier("withTracing") RestTemplate restTemplate,
                                         PnUserattributesConfig cfg) {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getClientTemplatesengineBasepath());
        return new TemplateApi(apiClient);
    }

}
