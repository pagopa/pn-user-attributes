package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        AddressBookEntityToLegalDigitalAddressDtoMapper.class
})
class AddressBookEntityToLegalDigitalAddressDtoMapperTest {

    @Autowired
    AddressBookEntityToLegalDigitalAddressDtoMapper mapper;

    @Test
    void toDto() {
        AddressBookEntity ce = AddressBookDaoTestIT.newAddress(true);

        LegalDigitalAddressDto dtoExpected = new LegalDigitalAddressDto();
        dtoExpected.setRecipientId(ce.getRecipientId());
        dtoExpected.setChannelType(LegalChannelTypeDto.fromValue(ce.getChannelType()));
        dtoExpected.setAddressType(LegalAddressTypeDto.fromValue(ce.getAddressType()));
        dtoExpected.setSenderId(ce.getSenderId());
        dtoExpected.setCreated(ce.getCreated());
        dtoExpected.setLastModified(ce.getLastModified());

        LegalDigitalAddressDto dto = mapper.toDto(ce);

        assertEquals(dtoExpected, dto);
    }
}