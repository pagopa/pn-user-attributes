package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.handler.PecValidationExpiredResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import it.pagopa.pn.user.attributes.utils.ConsumerMDCUtils;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@lombok.CustomLog
public class ActionHandler {
    private final IONotificationService ioNotificationService;
    private final PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler;


    public ActionHandler(IONotificationService ioNotificationService, PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler) {
        this.ioNotificationService = ioNotificationService;
        this.pecValidationExpiredResponseHandler = pecValidationExpiredResponseHandler;
    }

    @SqsListener(value = "${pn.user-attributes.actions}")
    public void pnUserAttributesSendMessageActionConsumer(Message<Action> message) {
        String process = "Managing app IO send message";
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
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
    }

    @SqsListener(value = "${pn.user-attributes.actions}")
    public void pnUserAttributesIoActivatedActionConsumer(Message<Action> message) {
        String process = "Managing app IO activated";
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
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
    }


    @SqsListener(value = "${pn.user-attributes.actions}")
    public void pnUserAttributesPecValidationExpiredActionConsumer(Message<Action> message) {
        String process = "Managing PEC validation expired";
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
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
    }
}
