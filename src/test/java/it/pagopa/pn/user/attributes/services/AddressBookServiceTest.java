package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.user.attributes.exceptions.InternalErrorException;
import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        PnAuditLogBuilder.class,
})
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
    PnExternalRegistryIoClient ioFunctionServicesClient;

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
    void saveCourtesyAddressBookEmail() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }


    @Test
    void saveCourtesyAddressBookSMS() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("3331234567");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @Test
    void saveCourtesyAddressBookAPPIO() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.empty()).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
    }


    @Test
    void saveCourtesyAddressBookAPPIO_FAIL() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.error(new RuntimeException()));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.empty());
        assertThrows(RuntimeException.class, () -> {
            mono.block(d);
        });


        //THEN
    }

    @Test
    void saveCourtesyAddressBookAPPIO_FAIL2() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(false));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.empty());
        assertThrows(InternalErrorException.class, () -> {
            mono.block(d);
        });


        //THEN
    }

    @Test
    void saveCourtesyAddressBookWithVerificationCode() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
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
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
    }

    @Test
    void saveCourtesyAddressBookWithInvalidVerificationCode() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
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
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.just(addressVerificationDto));
        assertThrows(InvalidVerificationCodeException.class, () -> mono.block(d));


        //THEN
    }

    @Test
    void saveCourtesyAddressBookWithAlreadyVerified() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
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
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, Mono.just(addressVerificationDto)).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
    }


    @Test
    void deleteLegalAddressBook() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(pnDatavaultClient.deleteRecipientAddressByInternalId(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Object result = addressBookService.deleteLegalAddressBook(recipientId, null, legalChannelType).block(d);

        //THEN
        assertNotNull( result );
    }

    @Test
    void deleteCourtesyAddressBook() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(pnDatavaultClient.deleteRecipientAddressByInternalId(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Object result = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType).block(d);

        //THEN
        assertNotNull( result );
    }


    @Test
    void deleteCourtesyAddressBookAPPIO() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(false));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Object result = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType).block(d);

        //THEN
        assertNotNull( result );
    }


    @Test
    void deleteCourtesyAddressBookAPPIO_FAIL() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.error(new RuntimeException()));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Mono<Object> mono = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType);
        assertThrows(RuntimeException.class, () -> {
            mono.block(d);
        });


        //THEN
    }



    @Test
    void deleteCourtesyAddressBookAPPIO_FAIL2() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Mono<Object> mono = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType);
        assertThrows(InternalErrorException.class, () -> {
            mono.block(d);
        });


        //THEN
    }

    @Test
    void getCourtesyAddressByRecipientAndSender() {
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

        final CourtesyDigitalAddressDto resdto1 = new CourtesyDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        resdto1.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);

        when(addressBookDao.getAddresses(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(),listFromDb.get(0).getSenderId()).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(result.get(0).getSenderId());
            Assertions.assertEquals(2, result.size());
            Assertions.assertTrue(result.contains(result.get(0)));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getCourtesyAddressByRecipient() {
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

        final CourtesyDigitalAddressDto resdto1 = new CourtesyDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        resdto1.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipient(listFromDb.get(0).getRecipientId()).collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getLegalAddressByRecipientAndSender() {
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
        resdto1.setChannelType(LegalChannelTypeDto.PEC);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.LEGAL);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.APPIO);



        when(addressBookDao.getAddresses(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        //When
        List<LegalDigitalAddressDto> result = addressBookService.getLegalAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(),listFromDb.get(0).getSenderId()).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(result.get(0).getSenderId());
            Assertions.assertEquals(2, result.size());
            Assertions.assertTrue(result.contains(result.get(0)));
        } catch (Exception e) {
            fail(e);
        }


    }

    @Test
    void getLegalAddressByRecipient() {
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
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalDigitalAddressDto.AddressTypeEnum.LEGAL);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.APPIO);


        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        //When
        List<LegalDigitalAddressDto> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId()).collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(2, result.size());


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


        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
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