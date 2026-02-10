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

    @SqsListener(value = "${pn.user-attributes.topics.actions}")
    public void pnUserAttributesActionConsumer(Message<Action> message) {
        String process = "Managing action";
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
        try {
            Action action = message.getPayload();
            MDC.put(MDCUtils.MDC_CX_ID_KEY, action.getInternalId());
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, action.getActionId());
            if (action.getSentNotification() != null) {
                MDC.put(MDCUtils.MDC_PN_IUN_KEY, action.getSentNotification().getIun());
            }
            log.logStartingProcess(process);
            log.debug("pnUserAttributesActionConsumer action={}", action);
            switch (action.getType()) {
                case SEND_MESSAGE_ACTION:
                    MDCUtils.addMDCToContextAndExecute(ioNotificationService.consumeIoSendMessageEvent(action.getInternalId(), action.getSentNotification())).block();
                    break;
                case IO_ACTIVATED_ACTION:
                    MDCUtils.addMDCToContextAndExecute(ioNotificationService.consumeIoActivationEvent(action.getActionId(), action.getInternalId(), action.getCheckFromWhen()).then()).block();
                    break;
                case PEC_REJECTED_ACTION:
                    MDCUtils.addMDCToContextAndExecute(pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent(action.getInternalId(), action.getAddress())).block();
                    break;
                default:
                    log.warn("Unknown action type: {}", action.getType());
            }
            log.logEndingProcess(process);
        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            log.logEndingProcess(process, false, ex.getMessage());
            throw ex;
        }
    }
}
