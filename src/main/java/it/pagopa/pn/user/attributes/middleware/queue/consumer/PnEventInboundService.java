/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.MDCWebFilter;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_EVENT_TYPE_MISSING;
import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_INVALID_EVENT_TYPE;

import java.util.UUID;

@Configuration
@Slf4j
public class PnEventInboundService {


    @Bean
    public MessageRoutingCallback customRouter() {
       return message -> {
           MessageHeaders messageHeaders = message.getHeaders();
           String traceId = "";

           if (messageHeaders.containsKey("aws_messageId"))
               traceId = messageHeaders.get("aws_messageId", String.class);
           else
               traceId = "traceId:" + UUID.randomUUID().toString();

           MDC.put(MDCWebFilter.MDC_TRACE_ID_KEY, traceId);

           return handleMessage(message);
       };
    }

    private String handleMessage(Message<?> message) {
        log.debug("messaggio ricevuto da customRouter "+message);
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("messaggio ricevuto da customRouter eventType={}", eventType );
        if(eventType != null){
            if(ActionType.IO_ACTIVATED_ACTION.name().equals(eventType))
                return "pnUserAttributesIoActivatedActionConsumer";
            else if(ActionType.SEND_MESSAGE_ACTION.name().equals(eventType))
                return "pnUserAttributesSendMessageActionConsumer";
        }else {
            log.error("eventType not present, cannot start scheduled action headers={} payload={}", message.getHeaders(), message.getPayload());
            throw new PnInternalException("eventType not present, cannot start scheduled action", ERROR_CODE_USERATTRIBUTES_EVENT_TYPE_MISSING);
        }

        throw new PnInternalException("eventType is not valid, cannot start scheduled action", ERROR_CODE_USERATTRIBUTES_INVALID_EVENT_TYPE);
    }

    
}
