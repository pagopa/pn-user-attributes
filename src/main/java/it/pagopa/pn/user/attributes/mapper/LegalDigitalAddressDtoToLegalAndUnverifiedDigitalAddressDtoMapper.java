package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAndUnverifiedDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;


public class LegalDigitalAddressDtoToLegalAndUnverifiedDigitalAddressDtoMapper {

    private LegalDigitalAddressDtoToLegalAndUnverifiedDigitalAddressDtoMapper(){
        super();
    }     

    public static LegalAndUnverifiedDigitalAddressDto toDto(LegalDigitalAddressDto entity) {
        LegalAndUnverifiedDigitalAddressDto dto = new LegalAndUnverifiedDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(entity.getChannelType());
        dto.setAddressType(LegalAddressTypeDto.LEGAL);
        dto.senderId(entity.getSenderId());
        dto.setValue(entity.getValue());
        dto.setSenderName(entity.getSenderName());
        dto.setCodeValid(true);
        dto.setPecValid(true);

        return  dto;
    }
}
