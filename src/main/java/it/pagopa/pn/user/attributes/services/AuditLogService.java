package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditLogService {

    public PnAuditLogEvent buildAuditLogEventWithIUN(String iun, Integer recIndex, PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments) {
        String logmessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{} - iun={} id={}", logmessage, iun, recIndex)
                .iun(iun)
                .build();
        logEvent.log();
        return logEvent;
    }

    public PnAuditLogEvent buildAuditLogEvent(PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments) {
        String logmessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{}", logmessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}
