package it.pagopa.pn.user.attributes.middleware.db.entities;

import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VerifiedAddressEntityTest {

    private VerifiedAddressEntity verifiedAddress;

    private String recipientId = "recipientIDTest";
    private String address = "addressTest";
    private String channelType = "channelTest";

    @BeforeEach
    void setUp() {
        verifiedAddress = new VerifiedAddressEntity(recipientId,address,channelType);
    }


    @Test
    void noArgConstr() {
        VerifiedAddressEntity verifiedAddressOne = new VerifiedAddressEntity();
        VerifiedAddressEntity verifiedAddressTwo = new VerifiedAddressEntity();
        boolean equals = verifiedAddressOne.equals(verifiedAddressTwo);
        assertTrue(equals);
    }

    @Test
    void getRecipientId() {
        assertEquals(verifiedAddress.getRecipientId(),recipientId);
    }

    @Test
    void getChannelType() {
        assertEquals(verifiedAddress.getChannelType(),channelType);
    }

    @Test
    void getHashedAddress() {
        assertEquals(verifiedAddress.getHashedAddress(),address);
    }

    @Test
    void testToString() {
        assertNotNull(verifiedAddress.toString());
    }
    

}