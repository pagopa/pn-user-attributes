package it.pagopa.pn.user.attributes.middleware.queue.entities;


import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV21;
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
    private SentNotificationV21 sentNotification = new SentNotificationV21();
    private ActionType actionType = ActionType.IO_ACTIVATED_ACTION;

    @BeforeEach
    void setUp() {
        action = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType, null);
    }

    @Test
    void testEquals() {
        Action toCompare = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType, null);
        boolean equals = action.equals(toCompare);
        assertTrue(equals);
    }

    @Test
    void canEqual() {
        Action toCompare = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType, null);
        boolean equals = action.canEqual(toCompare);
        assertTrue(equals);
    }

    @Test
    void testNotEquals() {
        Action toCompare = new Action("NOT",internalId,checkFromWhen,sentNotification,timeStamp,actionType, null);
        boolean equals = action.equals(toCompare);
        assertFalse(equals);
    }

    @Test
    void NotcanEqual() {
        Object toCompare = new Object();
        boolean equals = action.canEqual(toCompare);
        assertFalse(equals);
    }

    @Test
    void builder() {
        Action toCompare = Action.builder().actionId(actionId).internalId(internalId)
                .checkFromWhen(checkFromWhen).sentNotification(sentNotification)
                .timestamp(timeStamp).type(actionType).build();
        assertEquals(action,toCompare);
    }

    @Test
    void testNoArgConstr() {
        Action actionNoargs = new Action();
        assertNotNull(actionNoargs);
    }


    @Test
    void testHashCode() {
        Action toCompare = new Action(actionId,internalId,checkFromWhen,sentNotification,timeStamp,actionType, null);
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