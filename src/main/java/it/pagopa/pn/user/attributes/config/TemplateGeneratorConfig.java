package it.pagopa.pn.user.attributes.config;

import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.middleware.templates.impl.TemplateGeneratorByClient;
import it.pagopa.pn.user.attributes.middleware.templates.impl.TemplateGeneratorByDocComposition;
import it.pagopa.pn.user.attributes.middleware.wsclient.TemplatesClientImpl;
import it.pagopa.pn.user.attributes.utils.DocumentComposition;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class TemplateGeneratorConfig {

    private final DocumentComposition documentComposition;
    private final TemplatesClientImpl templatesClient;

    private final PnUserattributesConfig pnUserattributesConfig;

    @Bean
    @ConditionalOnProperty(name = "pn.user-attributes.enableTemplatesEngine", havingValue = "true", matchIfMissing = true)
    public TemplateGenerator templateGeneratorClient() {
        return new TemplateGeneratorByClient(templatesClient);
    }

    @Bean
    @ConditionalOnProperty(name = "pn.user-attributes.enableTemplatesEngine", havingValue = "false")
    public TemplateGenerator templateGeneratorDocComposition() {
        return new TemplateGeneratorByDocComposition(documentComposition, pnUserattributesConfig);
    }

}
