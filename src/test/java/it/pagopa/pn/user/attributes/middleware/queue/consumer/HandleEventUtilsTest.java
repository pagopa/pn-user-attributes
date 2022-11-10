package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class HandleEventUtilsTest {

    private MessageHeaders messageHeaders;
    private HashMap<String,Object> headers = new HashMap<>();
    private String eventID ="eventIDTest";
    private String createdAt = "2033-10-11T19:34:50.63Z";
    private String eventType ="eventTypeTest";
    private String publisher ="publisherTest";


    @BeforeEach
    void setUp() {
        headers.put("PN_EVENT_HEADER_EVENT_ID",eventID);
        headers.put("PN_EVENT_HEADER_EVENT_TYPE",eventType);
        headers.put("PN_EVENT_HEADER_CREATED_AT",createdAt);
        headers.put("PN_EVENT_HEADER_PUBLISHER",publisher);
    }

    @Test
    void mapStandardEventHeaderHeadersNotNull() {
        messageHeaders = new MessageHeaders(headers);
        assertNotNull(HandleEventUtils.mapStandardEventHeader(messageHeaders));
    }

    @Test
    void mapStandardEventHeaderHeadersNull() {
        assertThrows(PnInternalException.class,()->HandleEventUtils.mapStandardEventHeader(null));
    }

    @Test
    void handleExceptionHeadersNotNull(CapturedOutput output) {
        messageHeaders = new MessageHeaders(headers);
        HandleEventUtils.handleException(messageHeaders,new Exception());
        assertTrue(output.getOut().contains("Generic exception for iun"));
    }

    @Test
    void handleExceptionHeadersNull(CapturedOutput output) {
        HandleEventUtils.handleException(null,new Exception());
        assertTrue(output.getOut().contains("Generic exception ex"));
    }
}