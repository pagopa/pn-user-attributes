package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsentActionDtoToConsentEntityMapper {

    public ConsentActionDtoToConsentEntityMapper(){
        super();
    }

    public ConsentEntity toEntity(String recipientId, ConsentTypeDto consentType, ConsentActionDto dto) {
        ConsentEntity entity = new ConsentEntity(recipientId, consentType.getValue());
        entity.setAccepted(dto.getAction().equals(ConsentActionDto.ActionEnum.ACCEPT));
        return entity;
    }
}
