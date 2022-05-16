package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.mail.Address;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AddressBookServiceTest {
    @InjectMocks
    private AddressBookService addressBookService;

    @Mock
    PnDataVaultClient pnDatavaultClient;

    @Mock
    AddressBookDao addressBookDao;

    @Mock
    PnExternalChannelClient pnExternalChannelClient;

    @Mock
    AddressBookEntityToCourtesyDigitalAddressDtoMapper courtesyDigitalAddressToDto;

    @Mock
    AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;

    @Test
    void saveLegalAddressBook() {
    }

    @Test
    void saveCourtesyAddressBook() {
    }

    @Test
    void deleteLegalAddressBook() {
    }

    @Test
    void deleteCourtesyAddressBook() {
    }

    @Test
    void getCourtesyAddressByRecipientAndSender() {
    }

    @Test
    void getCourtesyAddressByRecipient() {
    }

    @Test
    void getLegalAddressByRecipientAndSender() {
    }

    @Test
    void getLegalAddressByRecipient() {
    }

    @Test
    void getAddressesByRecipient() {
        //Given
        AddressBookEntity entity = AddressBookDaoTestIT.newAddress(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final LegalDigitalAddressDto legalAddressDto = new LegalDigitalAddressDto();
        legalAddressDto.setRecipientId(entity.getRecipientId());
        legalAddressDto.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.LEGAL);
        legalAddressDto.setSenderId("default");
        legalAddressDto.setChannelType(LegalChannelTypeDto.PEC);

        final CourtesyDigitalAddressDto courtesyAddressDto = new CourtesyDigitalAddressDto();
        courtesyAddressDto.setRecipientId(entity.getRecipientId());
        courtesyAddressDto.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        courtesyAddressDto.setSenderId("default");
        courtesyAddressDto.setChannelType(CourtesyChannelTypeDto.EMAIL);

        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);


        when(addressBookDao.getAllAddressesByRecipient (Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());


        //When
        List<AddressBookEntity> result = addressBookService.getAddressesByRecipient(entity.getRecipientId().block(Duration.ofMillis(3000)));

        //Then
        assertNotNull(result);

        assertEquals(2, result.size());

    }

}