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

/**
 * Classe di configurazione per impostare i bean di Template Generator in base alle proprietà dell'applicazione.
 * <p>
 * Questa configurazione determina quale implementazione dell'interfaccia {@link TemplateGenerator}
 * deve essere istanziata a seconda del valore della proprietà
 * {@code pn.user-attributes.enableTemplatesEngine}.
 * </p>
 *
 * <p>
 * Quando la proprietà è impostata su {@code true} (o non è definita), viene utilizzata
 * l'implementazione {@link TemplateGeneratorByClient}, che si basa su {@link TemplatesClientImpl}.
 * Quando la proprietà è impostata su {@code false}, viene utilizzata l'implementazione
 * {@link TemplateGeneratorByDocComposition}, che utilizza {@link DocumentComposition} e
 * {@link PnUserattributesConfig}.
 * </p>
 *
 * <p>
 * **Nota:** Quando l'implementazione {@link TemplateGeneratorByDocComposition} non sarà più necessaria,
 * si dovranno eliminare {@link TemplateGeneratorConfig}, {@link TemplateGeneratorByDocComposition} e qualsiasi riferimento ad esse
 * come ad esempio la properties pn.user-attributes.enableTemplatesEngine.
 * </p>
 */
@Configuration
@AllArgsConstructor
public class TemplateGeneratorConfig {

    /**
     * Servizio per la composizione dei documenti utilizzato da {@link TemplateGeneratorByDocComposition}.
     */
    private final DocumentComposition documentComposition;
    /**
     * Configurazione degli attributi utente, utilizzata da {@link TemplateGeneratorByDocComposition}.
     */
    private final PnUserattributesConfig pnUserattributesConfig;

    /**
     * Implementazione del client utilizzata da {@link TemplateGeneratorByClient}.
     */
    private final TemplatesClientImpl templatesClient;


    /**
     * Crea un bean di tipo {@link TemplateGeneratorByClient} quando la proprietà
     * {@code pn.user-attributes.enableTemplatesEngine} è impostata su {@code true} o non è definita.
     *
     * @return un'istanza di {@link TemplateGeneratorByClient}.
     */
    @Bean
    @ConditionalOnProperty(name = "pn.user-attributes.enableTemplatesEngine", havingValue = "true", matchIfMissing = true)
    public TemplateGenerator templateGeneratorClient() {
        return new TemplateGeneratorByClient(templatesClient);
    }

    /**
     * Crea un bean di tipo {@link TemplateGeneratorByDocComposition} quando la proprietà
     * {@code pn.user-attributes.enableTemplatesEngine} è impostata su {@code false}.
     *
     * <p>
     * **Nota:** Quando l'implementazione {@link TemplateGeneratorByDocComposition} sarà deprecata o rimossa,
     * sarà necessario eliminare questo metodo e le dipendenze correlate (es. {@link DocumentComposition})
     * da questa classe di configurazione.
     * </p>
     *
     * @return un'istanza di {@link TemplateGeneratorByDocComposition}.
     */
    @Bean
    @ConditionalOnProperty(name = "pn.user-attributes.enableTemplatesEngine", havingValue = "false")
    public TemplateGenerator templateGeneratorDocComposition() {
        return new TemplateGeneratorByDocComposition(documentComposition, pnUserattributesConfig);
    }

}
