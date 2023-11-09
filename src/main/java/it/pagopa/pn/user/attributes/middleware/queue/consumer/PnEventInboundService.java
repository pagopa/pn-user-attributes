/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_EVENT_TYPE_MISSING;

@Configuration
@Slf4j
public class PnEventInboundService {

    private final String externalChannelEventQueueName;

    public PnEventInboundService(PnUserattributesConfig cfg) {
        this.externalChannelEventQueueName = cfg.getTopics().getFromexternalchannel();
    }

    @Bean
    public MessageRoutingCallback customRouter() {
        return new MessageRoutingCallback() {
            @Override
            public FunctionRoutingResult routingResult(Message<?> message) {
                MessageHeaders messageHeaders = message.getHeaders();

                String traceId = null;
                String messageId = null;

                if (messageHeaders.containsKey("aws_messageId"))
                    messageId = messageHeaders.get("aws_messageId", String.class);
                if (messageHeaders.containsKey("X-Amzn-Trace-Id"))
                    traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);

                traceId = Objects.requireNonNullElseGet(traceId, () -> "traceId:" + UUID.randomUUID());

                MDCUtils.clearMDCKeys();
                MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
                MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, messageId);
                return new FunctionRoutingResult(handleMessage(message));
            }
        };
    }

    private String handleMessage(Message<?> message) {
        log.debug("messaggio ricevuto da customRouter message");
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("messaggio ricevuto da customRouter eventType={}", eventType );
        if(eventType != null){
            if(ActionType.IO_ACTIVATED_ACTION.name().equals(eventType))
                return "pnUserAttributesIoActivatedActionConsumer";
            else if(ActionType.SEND_MESSAGE_ACTION.name().equals(eventType))
                return "pnUserAttributesSendMessageActionConsumer";
            else if(ActionType.PEC_REJECTED_ACTION.name().equals(eventType))
                return "pnUserAttributesPecValidationExpiredActionConsumer";
            else if(eventType.equals("EXTERNAL_CHANNELS_EVENT")) {
                return "pnExternalChannelEventConsumer";
            }
            else
            {
                log.error("eventType not recognized, cannot start scheduled action headers={} payload={}", message.getHeaders(), message.getPayload());
                throw new PnInternalException("eventType not present, cannot start scheduled action", ERROR_CODE_USERATTRIBUTES_EVENT_TYPE_MISSING);
            }
        }else {
            return handleOtherEvent(message);
        }
    }


    @NotNull
    private String handleOtherEvent(Message<?> message) {

        String queueName = (String) message.getHeaders().get("aws_receivedQueue");
        if (Objects.equals(queueName, externalChannelEventQueueName)) {
            return "pnExternalChannelEventConsumer";
        }

        log.error("eventType not present, cannot start scheduled action headers={} payload={}", message.getHeaders(), message.getPayload());
        throw new PnInternalException("eventType not present, cannot start scheduled action", ERROR_CODE_USERATTRIBUTES_EVENT_TYPE_MISSING);

    }

    
}
