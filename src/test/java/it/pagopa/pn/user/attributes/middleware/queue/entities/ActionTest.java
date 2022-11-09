package it.pagopa.pn.user.attributes.middleware.queue.entities;


import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


class ActionTest {

    private Action action;

    private String actionId = "testID";
    private String internalId = "testInternalID";
    private Instant checkFromWhen = Instant.now();
    private Instant timeStamp = Instant.now();
    private SentNotification sentNotification = new SentNotification();
    private ActionType actionType = ActionType.IO_ACTIVATED_ACTION;

    @BeforeEach
    void setUp() {
        action = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType);
    }

    @Test
    void testEquals() {
        Action toCompare = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType);
        assertEquals(action,toCompare);
    }


    @Test
    void testHashCode() {
        Action toCompare = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType);
        assertEquals(action.hashCode(),toCompare.hashCode());
    }

    @Test
    void getActionId() {
        assertEquals(action.getActionId(),actionId);
    }

    @Test
    void getInternalId() {
        assertEquals(action.getInternalId(),internalId);
    }

    @Test
    void getCheckFromWhen() {
        assertEquals(action.getCheckFromWhen(),checkFromWhen);
    }

    @Test
    void getSentNotification() {
        assertEquals(action.getSentNotification(),sentNotification);
    }

    @Test
    void getTimestamp() {
        assertEquals(action.getTimestamp(),timeStamp);
    }

    @Test
    void getType() {
        assertEquals(action.getType(),actionType);
    }

    @Test
    void testToString() {
        assertNotNull(action.toString());
    }


}