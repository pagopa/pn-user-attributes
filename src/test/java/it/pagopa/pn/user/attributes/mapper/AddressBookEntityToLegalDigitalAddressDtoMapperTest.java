package it.pagopa.pn.user.attributes.mapper;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AddressBookEntityToLegalDigitalAddressDtoMapperTest {

    @Autowired
    AddressBookEntityToLegalDigitalAddressDtoMapper mapper;

    @Test
    void toDto() {
        AddressBookEntity ce = AddressBookDaoTestIT.newAddress(true);

        LegalDigitalAddressDto dtoExpected = new LegalDigitalAddressDto();
        dtoExpected.setRecipientId(ce.getRecipientId());
        dtoExpected.setChannelType(LegalChannelTypeDto.fromValue(ce.getChannelType()));
        dtoExpected.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.fromValue(ce.getAddressType()));
        dtoExpected.setSenderId(ce.getSenderId());

        LegalDigitalAddressDto dto = mapper.toDto(ce);

        assertEquals(dtoExpected, dto);
    }
}