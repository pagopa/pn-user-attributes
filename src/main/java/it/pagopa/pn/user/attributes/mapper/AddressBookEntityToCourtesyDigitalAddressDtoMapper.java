package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyDigitalAddressDto;
import org.springframework.stereotype.Component;

@Component
public class AddressBookEntityToCourtesyDigitalAddressDtoMapper {

    private AddressBookEntityToCourtesyDigitalAddressDtoMapper(){
        super();
    }     

    public CourtesyDigitalAddressDto toDto(AddressBookEntity entity) {
        CourtesyDigitalAddressDto dto = new CourtesyDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(CourtesyChannelTypeDto.fromValue(entity.getChannelType()));
        dto.setAddressType(CourtesyAddressTypeDto.COURTESY);
        dto.senderId(entity.getSenderId());
        dto.setCreated(entity.getCreated());
        dto.setLastModified(entity.getLastModified());
        return  dto;
    }
}
