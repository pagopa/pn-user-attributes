package it.pagopa.pn.user.attributes.middleware.queue.entities;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ActionEventTest {

    private ActionEvent actionEvent;

    private Action action = new Action("testID","testInternalID",
            Instant.now(),new SentNotification(),Instant.now(),ActionType.IO_ACTIVATED_ACTION);
    private StandardEventHeader standardEventHeader = new StandardEventHeader();

    @BeforeEach
    void setUp() {
        actionEvent = new ActionEvent(standardEventHeader,action);
    }


    @Test
    void testEquals() {
        ActionEvent toCompare = new ActionEvent(standardEventHeader,action);
        boolean equals = actionEvent.equals(toCompare);
        assertTrue(equals);
    }


    @Test
    void testHashCode() {
        ActionEvent toCompare = new ActionEvent(standardEventHeader,action);
        assertEquals(actionEvent.hashCode(),toCompare.hashCode());
    }

    @Test
    void getHeader() {
        assertEquals(actionEvent.getHeader(),standardEventHeader);
    }

    @Test
    void getPayload() {
        assertEquals(actionEvent.getPayload(),action);
    }

    @Test
    void testToString() {
        assertNotNull(actionEvent.toString());
    }


    @Test
    void canEqual() {
        ActionEvent toCompare = new ActionEvent(standardEventHeader,action);
        boolean equals = actionEvent.canEqual(toCompare);
        assertTrue(equals);
    }

    @Test
    void builder() {
        ActionEvent toCompare = ActionEvent.builder().header(standardEventHeader).payload(action).build();
        assertEquals(actionEvent,toCompare);
    }

}