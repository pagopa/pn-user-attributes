package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyAddressTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyAndUnverifiedDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;


public class VerificationCodeEntityToCourtesyAndUnverifiedDigitalAddressDtoMapper {

    private VerificationCodeEntityToCourtesyAndUnverifiedDigitalAddressDtoMapper(){
        super();
    }     

    public static CourtesyAndUnverifiedDigitalAddressDto toDto(VerificationCodeEntity entity) {
        CourtesyAndUnverifiedDigitalAddressDto dto = new CourtesyAndUnverifiedDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(CourtesyChannelTypeDto.fromValue(entity.getChannelType()));
        dto.setAddressType(CourtesyAddressTypeDto.COURTESY);
        dto.senderId(entity.getSenderId());
        dto.setRequestId(entity.getRequestId());
        dto.setCodeValid(entity.isCodeValid());

        return  dto;
    }
}
