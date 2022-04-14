package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConsentEntityTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CONSENTTYPE = ConsentTypeDto.TOS.toString();

    @BeforeEach
    void setUp() {
    }

    @Test
    void getPk() {
//        ConsentEntity ce = new ConsentEntity();
//        ce.setRecipientId(RECIPIENTID);
//        ce.setConsentType(CONSENTTYPE);
//        ce.get
        String pk = ConsentEntity.getPk(RECIPIENTID);
        String pkExpected = "CO#" + RECIPIENTID;

        assertEquals(pkExpected, pk);
    }

    @Test
    void getRecipientIdNoPrefix() {
        String pk = ConsentEntity.getPk(RECIPIENTID);
        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(pk);
        String recIdRead = ce.getRecipientIdNoPrefix();
        assertEquals(RECIPIENTID, recIdRead);
    }

    @Test
    void setRecipientId() {
        String pk = ConsentEntity.getPk(RECIPIENTID);
        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(pk);
        String recIdExpected = ce.getRecipientId();
        assertEquals(recIdExpected, pk);
    }

    @Test
    void setConsentType() {
        ConsentEntity ce = new ConsentEntity();
        ce.setConsentType(CONSENTTYPE);
        String typeRead = ce.getConsentType();
        assertEquals(CONSENTTYPE, typeRead);
    }

    @Test
    void setCreated() {
        Instant time = Instant.now();

        ConsentEntity ce = new ConsentEntity();
        ce.setCreated(time);
        Instant timeExpected = ce.getCreated();
        assertEquals(timeExpected, time);
    }

    @Test
    void setLastModified() {
        Instant time = Instant.now();

        ConsentEntity ce = new ConsentEntity();
        ce.setLastModified(time);
        Instant timeExpected = ce.getLastModified();
        assertEquals(timeExpected, time);
    }

    @Test
    void setAccepted() {
        boolean accepted = true;
        ConsentEntity ce = new ConsentEntity();
        ce.setAccepted(accepted);
        boolean acceptedExpected = ce.isAccepted();
        assertEquals(acceptedExpected, accepted);
    }

    @Test
    void getRecipientId() {
        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(RECIPIENTID);
        String recidRead = ce.getRecipientId();
        assertEquals(RECIPIENTID, recidRead);
    }

    @Test
    void getConsentType() {
        ConsentEntity ce = new ConsentEntity();
        ce.setConsentType(CONSENTTYPE);
        String ctypeRead = ce.getConsentType();
        assertEquals(CONSENTTYPE, ctypeRead);
    }

    @Test
    void getCreated() {
        Instant time = Instant.now();

        ConsentEntity ce = new ConsentEntity();
        ce.setCreated(time);
        Instant timeExpected = ce.getCreated();
        assertEquals(timeExpected, time);
    }

    @Test
    void getLastModified() {
        Instant time = Instant.now();

        ConsentEntity ce = new ConsentEntity();
        ce.setLastModified(time);
        Instant timeExpected = ce.getLastModified();
        assertEquals(timeExpected, time);
    }

    @Test
    void isAccepted() {
        boolean accepted = false;
        ConsentEntity ce = new ConsentEntity();
        ce.setAccepted(accepted);
        boolean acceptedExpected = ce.isAccepted();
        assertEquals(acceptedExpected, accepted);
    }
}