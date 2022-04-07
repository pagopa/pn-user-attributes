package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsentActionDtoToConsentEntityMapper {

    private ConsentActionDtoToConsentEntityMapper(){
        super();
    }

    public ConsentEntity toEntity(String recipientId, ConsentTypeDto consentType, ConsentActionDto dto) {
        ConsentEntity entity = new ConsentEntity();
        entity.setRecipientId(ConsentEntity.getPk(recipientId)); // pk
        entity.setConsentType(consentType.getValue()); //sk
        entity.setAccepted(dto.getAction().equals(ConsentActionDto.ActionEnum.ACCEPT) ? true : false);
        return entity;
    }
}
