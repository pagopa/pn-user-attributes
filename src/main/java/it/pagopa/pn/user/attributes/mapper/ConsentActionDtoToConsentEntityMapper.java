package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import org.springframework.stereotype.Component;

@Component
public class ConsentActionDtoToConsentEntityMapper {

    public ConsentActionDtoToConsentEntityMapper(){
        super();
    }

    public ConsentEntity toEntity(String recipientId, ConsentTypeDto consentType, ConsentActionDto dto, String version) {
        ConsentEntity entity = new ConsentEntity(recipientId, consentType.getValue(), version);
        entity.setAccepted(dto.getAction().equals(ConsentActionDto.ActionEnum.ACCEPT));
        return entity;
    }
}
