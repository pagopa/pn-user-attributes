package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.handler.PecValidationExpiredResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@lombok.CustomLog
public class ActionHandler {
    private final IONotificationService ioNotificationService;
    private final PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler;


    public ActionHandler(IONotificationService ioNotificationService, PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler) {
        this.ioNotificationService = ioNotificationService;
        this.pecValidationExpiredResponseHandler = pecValidationExpiredResponseHandler;
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesSendMessageActionConsumer() {
        return message -> {
            String process = "Managing app IO send message";
            try {
                Action action = message.getPayload();
                MDC.put(MDCUtils.MDC_PN_IUN_KEY, action.getSentNotification().getIun());
                MDC.put(MDCUtils.MDC_CX_ID_KEY, action.getInternalId());
                MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, action.getActionId());

                // messo lo starting process dopo, così nei log ha MDC aggiornato
                log.logStartingProcess(process);
                log.debug("pnUserAttributesSendMessageActionConsumer action={}", action);
                MDCUtils.addMDCToContextAndExecute(ioNotificationService.consumeIoSendMessageEvent(action.getInternalId(), action.getSentNotification())).block();
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
                Action action = message.getPayload();
                MDC.put(MDCUtils.MDC_CX_ID_KEY, action.getInternalId());
                MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, action.getActionId());
                // messo lo starting process dopo, così nei log ha MDC aggiornato
                log.logStartingProcess(process);
                log.debug("pnUserAttributesIoActivatedActionConsumer action={}", action);
                MDCUtils.addMDCToContextAndExecute(ioNotificationService.consumeIoActivationEvent(action.getActionId(), action.getInternalId(), action.getCheckFromWhen()).then()).block();
                log.logEndingProcess(process);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                log.logEndingProcess(process, false, ex.getMessage());
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnUserAttributesPecValidationExpiredActionConsumer() {
        return message -> {
            String process = "Managing PEC validation expired";
            try {
                Action action = message.getPayload();
                MDC.put(MDCUtils.MDC_CX_ID_KEY, action.getInternalId());
                MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, action.getActionId());

                // messo lo starting process dopo, così nei log ha MDC aggiornato
                log.logStartingProcess(process);
                log.debug("pnUserAttributesPecValidationExpiredActionConsumer action={}", action);
                MDCUtils.addMDCToContextAndExecute(pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent(action.getInternalId(), action.getAddress())).block();
                log.logEndingProcess(process);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                log.logEndingProcess(process, false, ex.getMessage());
                throw ex;
            }
        };
    }
}
