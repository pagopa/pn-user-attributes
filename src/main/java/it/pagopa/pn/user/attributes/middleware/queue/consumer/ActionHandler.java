package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@lombok.CustomLog
public class ActionHandler {
    private final IONotificationService ioNotificationService;


    public ActionHandler(IONotificationService ioNotificationService) {
        this.ioNotificationService = ioNotificationService;
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesSendMessageActionConsumer() {
        return message -> {
            String process = "Managing app IO send message";
            try {
                log.logStartingProcess(process);
                Action action = message.getPayload();
                log.debug("pnUserAttributesSendMessageActionConsumer action={}", action);
                ioNotificationService.consumeIoSendMessageEvent(action.getInternalId(), action.getSentNotification()).block();
                log.logEndingProcess(process);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                log.logEndingProcess(process, false, ex.getMessage());
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesIoActivatedActionConsumer() {
        return message -> {
            String process = "Managing app IO activated";
            try {
                log.logStartingProcess(process);
                Action action = message.getPayload();
                log.debug("pnUserAttributesIoActivatedActionConsumer action={}", action);
                ioNotificationService.consumeIoActivationEvent(action.getInternalId(), action.getCheckFromWhen()).then().block();
                log.logEndingProcess(process);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                log.logEndingProcess(process, false, ex.getMessage());
                throw ex;
            }
        };
    }

}
