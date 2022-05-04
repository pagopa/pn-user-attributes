package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
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
        dto.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.fromValue(entity.getAddressType()));
        dto.senderId(entity.getSenderId());

        return  dto;
    }
}
