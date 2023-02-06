package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditLogServiceTest {

    AuditLogService auditLogService;

    @BeforeEach
    void beforeEach(){
        auditLogService = new AuditLogService();
    }

    @Test
    void buildAuditLogEventWithIUN() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEventWithIUN("iun1", 0, PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio");
        assertNotNull(event);
        event.generateSuccess().log();
    }

    @Test
    void buildAuditLogEventWithIUN2() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEventWithIUN("iun1", 0, PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio par1={} par2={}", "parametro1", 2);
        assertNotNull(event);
        event.generateSuccess().log();
    }


    @Test
    void buildAuditLogEventWithIUN0() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEventWithIUN("iun1", 0, PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio ");
        assertNotNull(event);
        event.generateSuccess().log();
    }


    @Test
    void buildAuditLogEvent() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEvent(PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio");
        assertNotNull(event);
        event.generateSuccess().log();
    }

    @Test
    void buildAuditLogEvent2() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEvent(PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio par1={} par2={}", "parametro1", 2);
        assertNotNull(event);
        event.generateSuccess().log();
    }


    @Test
    void buildAuditLogEvent0() {

        PnAuditLogEvent event = auditLogService.buildAuditLogEvent(PnAuditLogEventType.AUD_DA_SEND_IO,"messaggio ");
        assertNotNull(event);
        event.generateSuccess().log();
    }
}