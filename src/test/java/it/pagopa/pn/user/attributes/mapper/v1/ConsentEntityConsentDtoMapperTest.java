package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ConsentEntityConsentDtoMapperTest {

    @Test
    void toDto() {
        String recipientId = "1234";
        ConsentTypeDto type = ConsentTypeDto.TOS;
        boolean accepted = true;

        ConsentEntityConsentDtoMapper mapper = new ConsentEntityConsentDtoMapper();
        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(ConsentEntity.getPk(recipientId));
        ce.setAccepted(accepted);
        ce.setConsentType(type.getValue());
        ce.setCreated(Instant.now());

        ConsentDto dtoExpected = new ConsentDto();
        dtoExpected.setRecipientId(recipientId);
        dtoExpected.setAccepted(accepted);
        dtoExpected.setConsentType(type);

        ConsentDto dto = mapper.toDto(ce);

        assertEquals(dtoExpected, dto);

        ce.setAccepted(!accepted);
        dto = mapper.toDto(ce);

        assertNotEquals(dtoExpected, dto);
    }
}