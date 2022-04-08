package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressVerificationDtoToVerificationCodeEntityMapper {

    private AddressVerificationDtoToVerificationCodeEntityMapper(){
        super();
    }

    public VerificationCodeEntity toEntity(String recipientId, CourtesyChannelTypeDto channelType, AddressVerificationDto dto) {
        VerificationCodeEntity entity = new VerificationCodeEntity();
        entity.setPk(VerificationCodeEntity.getPk(recipientId, channelType.getValue(), dto.getValue()));
        entity.setSk(VerificationCodeEntity.SK_VALUE);
        entity.setValidationCode(dto.getVerificationCode()); // pk
        return entity;
    }
}
