package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ActionHandler {
    private final IONotificationService ioNotificationService;

    public ActionHandler(IONotificationService ioNotificationService) {
        this.ioNotificationService = ioNotificationService;
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesSendMessageActionConsumer() {
        return message -> {
            try {
                log.info("pnUserAttributesSendMessageActionConsumer, message {}", message);
                Action action = message.getPayload();
                ioNotificationService. .handleRefinement(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesIoActivatedActionConsumer() {
        return message -> {
            try {
                log.info("pnUserAttributesIoActivatedActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

}
