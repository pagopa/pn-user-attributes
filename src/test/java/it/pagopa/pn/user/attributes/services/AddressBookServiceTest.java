package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.mail.Address;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AddressBookServiceTest {

    private final Duration d = Duration.ofMillis(3000);

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
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @Test
    void saveLegalAddressBookWithVerificationCode() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
    }

    @Test
    void saveLegalAddressBookWithInvalidVerificationCode() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("55555");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, Mono.just(addressVerificationDto));
        assertThrows(InvalidVerificationCodeException.class, () -> mono.block(d));


        //THEN
    }

    @Test
    void saveLegalAddressBookWithAlreadyVerified() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
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
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true));
        listFromDb.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(1).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        resdto1.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.LEGAL);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);

        final CourtesyDigitalAddressDto resdto2 = new CourtesyDigitalAddressDto();
        resdto2.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto2.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        resdto2.setSenderId(listFromDb.get(1).getSenderId());
        resdto2.setChannelType(CourtesyChannelTypeDto.EMAIL);


        when(addressBookDao.getAllAddressesByRecipient (Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto2);


        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDb.get(0).getRecipientId()).block(d);

        //Then
        assertNotNull(result);
        assertEquals(1, result.getLegal().size());
        assertEquals(1, result.getCourtesy().size());
    }

}