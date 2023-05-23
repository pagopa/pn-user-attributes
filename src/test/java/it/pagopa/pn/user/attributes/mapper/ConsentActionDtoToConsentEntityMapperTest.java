package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentActionDtoToConsentEntityMapperTest {

    @Test
    void toEntity() {
        //GIVEN
        String recipientId = "1234";
        ConsentTypeDto type = ConsentTypeDto.TOS;
        boolean accepted = true;

        ConsentActionDtoToConsentEntityMapper mapper = new ConsentActionDtoToConsentEntityMapper();

        ConsentActionDto dto = new ConsentActionDto();
        dto.setAction(ConsentActionDto.ActionEnum.ACCEPT);
        ConsentEntity ceExpected = new ConsentEntity(recipientId, type.getValue(),null);
        ceExpected.setAccepted(accepted);


        //WHEN
        ConsentEntity ce = mapper.toEntity(recipientId,type, dto, null);

        //THEN
        assertEquals(ceExpected.getRecipientId(), ce.getRecipientId());
        assertEquals(ceExpected.getConsentType(), ce.getConsentType());
    }
}