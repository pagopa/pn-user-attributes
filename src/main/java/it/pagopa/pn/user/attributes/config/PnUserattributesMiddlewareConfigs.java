package it.pagopa.pn.user.attributes.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnUserattributesMiddlewareConfigs {

    private final PnUserattributesConfig cfg;

    public PnUserattributesMiddlewareConfigs(PnUserattributesConfig cfg) {
        this.cfg = cfg;
    }

    @Bean
    public SqsActionProducer actionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getActions(), objMapper);
    }
}

