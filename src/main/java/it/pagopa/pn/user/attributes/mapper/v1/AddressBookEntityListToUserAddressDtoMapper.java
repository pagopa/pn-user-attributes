package it.pagopa.pn.user.attributes.mapper.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.*;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressBookEntityListToUserAddressDtoMapper {

    private AddressBookEntityListToUserAddressDtoMapper(){
        super();
    }     

    public UserAddressesDto toDto(List<AddressBookEntity> addressBookEntities) {
        UserAddressesDto uaDto = new UserAddressesDto();

        for (AddressBookEntity ent : addressBookEntities)
        {
            if (ent.getAddressType().equals(LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())) {
                LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
                dto.setRecipientId(ent.getRecipientId());
                dto.setCode(ent.getVerificationCode());
                dto.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.fromValue(ent.getAddressType()));
                dto.setValue(ent.getAddress());
                dto.senderId(ent.getSenderId());
                dto.setChannelType(LegalChannelTypeDto.fromValue(ent.getChannelType()));
                uaDto.addLegalItem(dto);
            } else {
                CourtesyDigitalAddressDto dto = new CourtesyDigitalAddressDto();
                dto.setRecipientId(ent.getRecipientId());
                dto.setCode(ent.getVerificationCode());
                dto.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.fromValue(ent.getAddressType()));
                dto.setValue(ent.getAddress());
                dto.senderId(ent.getSenderId());
                dto.setChannelType( CourtesyChannelTypeDto.fromValue(ent.getChannelType()));
                uaDto.addCourtesyItem(dto);
            }
        }

        return  uaDto;
    }
}
