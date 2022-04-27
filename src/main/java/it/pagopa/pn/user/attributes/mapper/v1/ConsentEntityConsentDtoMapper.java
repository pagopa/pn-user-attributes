package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsentEntityConsentDtoMapper {

    public ConsentEntityConsentDtoMapper(){
        super();
    }     

    public ConsentDto toDto(ConsentEntity entity) {
        ConsentDto dto = new ConsentDto();
        dto.setRecipientId(entity.getRecipientIdNoPrefix());
        dto.setAccepted(entity.isAccepted());
        dto.setConsentType(ConsentTypeDto.fromValue(entity.getConsentType()));
        return  dto;
    }
}
