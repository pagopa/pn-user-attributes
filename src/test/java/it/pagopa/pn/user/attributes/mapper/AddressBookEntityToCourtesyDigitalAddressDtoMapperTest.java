package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        AddressBookEntityToCourtesyDigitalAddressDtoMapper.class
})
class AddressBookEntityToCourtesyDigitalAddressDtoMapperTest {


    @Autowired
    AddressBookEntityToCourtesyDigitalAddressDtoMapper mapper;

    @Test
    void toDto() {
        AddressBookEntity ce = AddressBookDaoTestIT.newAddress(false);

        CourtesyDigitalAddressDto dtoExpected = new CourtesyDigitalAddressDto();
        dtoExpected.setRecipientId(ce.getRecipientId());
        dtoExpected.setChannelType(CourtesyChannelTypeDto.fromValue(ce.getChannelType()));
        dtoExpected.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.fromValue(ce.getAddressType()));
        dtoExpected.setSenderId(ce.getSenderId());

        CourtesyDigitalAddressDto dto = mapper.toDto(ce);

        assertEquals(dtoExpected, dto);
    }
}