package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressBookEntityToLegalDigitalAddressDtoMapper {

    private AddressBookEntityToLegalDigitalAddressDtoMapper(){
        super();
    }     

    public LegalDigitalAddressDto toDto(AddressBookEntity entity) {
        LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(LegalChannelTypeDto.fromValue(entity.getChannelType()));
        dto.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.fromValue(entity.getAddressType()));
        dto.senderId(entity.getSenderId());

        return  dto;
    }
}
