package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsentActionDtoToConsentEntityMapperTest {

    @Test
    void toEntity() {
        String recipientId = "1234";
        ConsentTypeDto type = ConsentTypeDto.TOS;
        boolean accepted = true;

        ConsentActionDtoToConsentEntityMapper mapper = new ConsentActionDtoToConsentEntityMapper();

        ConsentActionDto dto = new ConsentActionDto();
        dto.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = mapper.toEntity(recipientId,type, dto);

        ConsentEntity ceExpected = new ConsentEntity(recipientId, type.getValue());
        ceExpected.setAccepted(accepted);

        assertEquals(ceExpected, ce);

        ce = mapper.toEntity(recipientId,type, dto);
        ceExpected.setAccepted(!accepted);

        assertNotEquals(ceExpected, ce);

    }
}