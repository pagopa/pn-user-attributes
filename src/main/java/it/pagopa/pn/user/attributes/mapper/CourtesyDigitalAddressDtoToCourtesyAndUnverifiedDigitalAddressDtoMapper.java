package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyAddressTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyAndUnverifiedDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;


public class CourtesyDigitalAddressDtoToCourtesyAndUnverifiedDigitalAddressDtoMapper {

    private CourtesyDigitalAddressDtoToCourtesyAndUnverifiedDigitalAddressDtoMapper(){
        super();
    }     

    public static CourtesyAndUnverifiedDigitalAddressDto toDto(CourtesyDigitalAddressDto entity) {
        CourtesyAndUnverifiedDigitalAddressDto dto = new CourtesyAndUnverifiedDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(entity.getChannelType());
        dto.setAddressType(CourtesyAddressTypeDto.COURTESY);
        dto.senderId(entity.getSenderId());
        dto.setValue(entity.getValue());
        dto.setSenderName(entity.getSenderName());
        dto.setCodeValid(true);

        return  dto;
    }
}
