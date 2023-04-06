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
                log.info("[enter] pnUserAttributesSendMessageActionConsumer, message {}", message);
                Action action = message.getPayload();
                ioNotificationService.consumeIoSendMessageEvent(action.getInternalId(), action.getSentNotification()).block();
                log.info("[exit] pnUserAttributesSendMessageActionConsumer");
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
                log.info("[enter] pnUserAttributesIoActivatedActionConsumer, message {}", message);
                Action action = message.getPayload();
                ioNotificationService.consumeIoActivationEvent(action.getInternalId(), action.getCheckFromWhen()).then().block();
                log.info("[exit] pnUserAttributesIoActivatedActionConsumer");
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

}
