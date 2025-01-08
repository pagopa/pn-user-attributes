package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.api.TemplateApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;


@Configuration
public class TemplatesClientConfig {

    /**
     * Configures and provides a {@link TemplateApi} bean as the primary implementation.
     * <p>
     * This method sets up an instance of {@link TemplateApi} using a provided {@link RestTemplate}
     * and configuration properties from {@link PnUserattributesConfig}. It initializes the API client
     * with the base path required for HTTP communication with the templates engine microservice.
     * </p>
     *
     * @param restTemplate the {@link RestTemplate} to use for HTTP communication.
     * @param cfg the configuration properties for the templates engine service.
     * @return a fully configured {@link TemplateApi} instance.
     */
    @Bean
    @Primary
    public TemplateApi templateApiConfig(RestTemplate restTemplate,
                                         PnUserattributesConfig cfg) {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getClientTemplatesengineBasepath());
        return new TemplateApi(apiClient);
    }
}