package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAndUnverifiedDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;


public class VerificationCodeEntityToLegalAndUnverifiedDigitalAddressDtoMapper {

    private VerificationCodeEntityToLegalAndUnverifiedDigitalAddressDtoMapper(){
        super();
    }

    public static LegalAndUnverifiedDigitalAddressDto toDto(VerificationCodeEntity entity) {
        LegalAndUnverifiedDigitalAddressDto dto = new LegalAndUnverifiedDigitalAddressDto();
        dto.setRecipientId(entity.getRecipientId());
        dto.setChannelType(LegalChannelTypeDto.fromValue(entity.getChannelType()));
        dto.setAddressType(LegalAddressTypeDto.LEGAL);
        dto.senderId(entity.getSenderId());
        dto.setRequestId(entity.getRequestId());
        dto.setCodeValid(entity.isCodeValid());
        dto.setPecValid(entity.isPecValid());

        return  dto;
    }
}
