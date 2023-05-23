package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ExternalChannelHandler {

    private final ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelHandler(ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }


    @Bean
    public Consumer<Message<SingleStatusUpdateDto>> pnExternalChannelEventConsumer() {
        return message -> {
            try {
                log.info("[enter] pnExternalChannelEventConsumer, message {}", message);
                SingleStatusUpdateDto singleStatusUpdateDto = message.getPayload();
                externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto).then().block();
                log.info("[exit] pnExternalChannelEventConsumer");
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
