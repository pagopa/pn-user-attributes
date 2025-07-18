package it.pagopa.pn.user.attributes.config;

import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.middleware.templates.impl.TemplateGeneratorByClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.TemplatesClient;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@AllArgsConstructor
public class TemplateGeneratorConfig {

    /**
     * Implementazione del client utilizzata da {@link TemplateGeneratorByClient}.
     */
    private final TemplatesClient templatesClient;


    /**
     * Crea un bean di tipo {@link TemplateGeneratorByClient} quando la proprietà
     *
     * @return un'istanza di {@link TemplateGeneratorByClient}.
     */
    @Bean
    public TemplateGenerator templateGeneratorClient() {
        return new TemplateGeneratorByClient(templatesClient);
    }


}
