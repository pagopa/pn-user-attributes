package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ConsumerMDCUtils {
    private ConsumerMDCUtils() {}

    public static void addMessageHeadersToMDC(Map<String, Object> messageHeaders) {
        String traceId = null;
        String messageId = null;

        if (messageHeaders.containsKey("aws_messageId"))
            messageId = messageHeaders.get("aws_messageId").toString();
        if (messageHeaders.containsKey("X-Amzn-Trace-Id"))
            traceId = messageHeaders.get("X-Amzn-Trace-Id").toString();

        traceId = Objects.requireNonNullElseGet(traceId, () -> "traceId:" + UUID.randomUUID());

        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, messageId);
    }
}
