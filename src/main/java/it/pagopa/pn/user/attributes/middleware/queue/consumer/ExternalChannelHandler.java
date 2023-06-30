package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@lombok.CustomLog
public class ExternalChannelHandler {

    private final ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelHandler(ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }


    @Bean
    public Consumer<Message<SingleStatusUpdateDto>> pnExternalChannelEventConsumer() {
        return message -> {
            String process = "Managing ext-channel event";
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
                log.logEndingProcess(process, false, ex.getMessage());
                throw ex;
            }
        };
    }

    private String getRequestId(SingleStatusUpdateDto singleStatusUpdateDto) {
        if (singleStatusUpdateDto.getDigitalLegal() != null)
            return singleStatusUpdateDto.getDigitalLegal().getRequestId();
        else if (singleStatusUpdateDto.getDigitalCourtesy() != null)
            return singleStatusUpdateDto.getDigitalCourtesy().getRequestId();

        return null;
    }
}
