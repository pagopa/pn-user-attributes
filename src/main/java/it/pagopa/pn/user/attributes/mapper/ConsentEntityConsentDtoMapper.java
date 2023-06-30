package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
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
        dto.setConsentVersion(entity.getConsentVersion());
        return  dto;
    }
}
