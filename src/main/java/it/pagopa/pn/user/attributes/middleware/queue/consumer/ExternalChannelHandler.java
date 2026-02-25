package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.user.attributes.utils.ConsumerMDCUtils;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@lombok.CustomLog
public class ExternalChannelHandler {

    private final ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelHandler(ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }

    @SqsListener(value = "${pn.user-attributes.topics.fromexternalchannel}")
    public void pnExternalChannelEventConsumer(Message<SingleStatusUpdateDto> message) {
        String process = "Managing ext-channel event";
        ConsumerMDCUtils.addMessageHeadersToMDC(message.getHeaders());
        try {
            SingleStatusUpdateDto singleStatusUpdateDto = message.getPayload();
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, getRequestId(singleStatusUpdateDto));
            // messo lo starting process dopo, così nei log ha il requestid
            log.logStartingProcess(process);
            log.debug("pnExternalChannelEventConsumer, event={}", singleStatusUpdateDto);
            MDCUtils.addMDCToContextAndExecute(externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto).then()).block();
            log.logEndingProcess(process);
        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            log.logEndingProcess(process, false, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String getRequestId(SingleStatusUpdateDto singleStatusUpdateDto) {
        if (singleStatusUpdateDto.getDigitalLegal() != null)
            return singleStatusUpdateDto.getDigitalLegal().getRequestId();
        else if (singleStatusUpdateDto.getDigitalCourtesy() != null)
            return singleStatusUpdateDto.getDigitalCourtesy().getRequestId();

        return null;
    }
}
