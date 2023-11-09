package it.pagopa.pn.user.attributes.middleware.queue.entities;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV21;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActionTypeTest {

    private Action action;
    private String valueBuildAction;

    @BeforeEach
    void setUp() {
        action = new Action("testID","testInternalID",
                Instant.now(),new SentNotificationV21(),Instant.now(),ActionType.IO_ACTIVATED_ACTION);
    }


    @Test
    void buildActionIdIOActivatedAddress() {
        valueBuildAction = String.format("%s_io_activated_%s", action.getActionId(), action.getInternalId());
        assertNotNull(ActionType.IO_ACTIVATED_ACTION.buildActionId(action));
        assertEquals(ActionType.IO_ACTIVATED_ACTION.buildActionId(action),valueBuildAction);
    }

    @Test
    void buildActionIdSendMessageAction() {
        valueBuildAction = String.format("%s_send_message_%s", action.getActionId(), action.getInternalId());
        assertNotNull(ActionType.SEND_MESSAGE_ACTION.buildActionId(action));
        assertEquals(ActionType.SEND_MESSAGE_ACTION.buildActionId(action),valueBuildAction);
    }

    @Test
    void values() {
        assertNotNull(ActionType.values());
    }

    @Test
    void valueOfSendMessageAction() {
        assertNotNull(ActionType.valueOf("SEND_MESSAGE_ACTION"));
    }

    @Test
    void valueOfIOActivatedAddress() {
        assertNotNull(ActionType.valueOf("IO_ACTIVATED_ACTION"));
    }
}