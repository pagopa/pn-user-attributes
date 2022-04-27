package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressVerificationDtoToAddressBookEntityMapper {

    private AddressVerificationDtoToAddressBookEntityMapper(){
        super();
    }

    public AddressBookEntity toEntity(String recipientId,
                                      String senderId,
                                      boolean isLegal,
                                      String channelType,
                                      AddressVerificationDto dto)
    {
        AddressBookEntity entity = new AddressBookEntity();
        entity.setPk(AddressBookEntity.getPk(recipientId));
        if (isLegal) {
            entity.setSk(AddressBookEntity.getSk(LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue(),
                                                senderId,
                                                channelType));
        } else {
            entity.setSk(AddressBookEntity.getSk(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue(),
                    senderId,
                    channelType));
        }

        entity.setAddress(dto.getValue());
        entity.setVerificationCode(dto.getVerificationCode());
        return entity;
    }
}
