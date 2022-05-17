package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsentEntityConsentDtoMapper {

    public ConsentEntityConsentDtoMapper(){
        super();
    }     

    public ConsentDto toDto(ConsentEntity entity) {
        ConsentDto dto = new ConsentDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setAccepted(entity.isAccepted());
        dto.setConsentType(ConsentTypeDto.fromValue(entity.getConsentType()));
        return  dto;
    }
}
