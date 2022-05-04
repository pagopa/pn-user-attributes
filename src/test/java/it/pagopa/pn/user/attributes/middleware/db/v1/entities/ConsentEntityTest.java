package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentEntityTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CONSENTTYPE = ConsentTypeDto.TOS.toString();

    @BeforeEach
    void setUp() {
    }


    @Test
    void getRecipientIdNoPrefix() {
    }
}
