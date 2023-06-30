package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConsentEntityConsentDtoMapperTest {

    @Test
    void toDto() {
        String recipientId = "1234";
        ConsentTypeDto type = ConsentTypeDto.TOS;
        boolean accepted = true;

        ConsentEntityConsentDtoMapper mapper = new ConsentEntityConsentDtoMapper();
        ConsentEntity ce = new ConsentEntity(recipientId, type.getValue(), null);
        ce.setAccepted(accepted);
        ce.setCreated(Instant.now());

        ConsentDto dtoExpected = new ConsentDto();
        dtoExpected.setRecipientId(recipientId);
        dtoExpected.setAccepted(accepted);
        dtoExpected.setConsentType(type);
        dtoExpected.setConsentVersion(ConsentEntity.NONEACCEPTED_VERSION);

        ConsentDto dto = mapper.toDto(ce);

        assertEquals(dtoExpected, dto);

        ce.setAccepted(!accepted);
        dto = mapper.toDto(ce);

        assertNotEquals(dtoExpected, dto);
    }
}