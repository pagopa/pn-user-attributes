package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnExpiredVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidInputException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnRetryLimitVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientTypeDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.UserStatusResponse;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.selfcare.v1.dto.PaSummary;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnSelfcareClient;
import it.pagopa.pn.user.attributes.services.utils.AppIOUtils;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.services.utils.VerifiedAddressUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AddressBookServiceTest {

    private final Duration d = Duration.ofMillis(3000);


    private AddressBookService addressBookService;


    private VerificationCodeUtils verificationCodeUtils;

    private AppIOUtils appIOUtils;

    private VerifiedAddressUtils verifiedAddressUtils;

    @Mock
    PnDataVaultClient pnDatavaultClient;

    @Mock
    PnSelfcareClient pnSelfcareClient;

    @Mock
    AddressBookDao addressBookDao;


    @Mock
    IONotificationService ioNotificationService;

    @Mock
    PnExternalChannelClient pnExternalChannelClient;

    @Mock
    PnExternalRegistryIoClient ioFunctionServicesClient;

    @Mock
    PnUserattributesConfig pnUserattributesConfig;

    @Mock
    AddressBookEntityToCourtesyDigitalAddressDtoMapper courtesyDigitalAddressToDto;

    @Mock
    AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;

    @BeforeEach
    void beforeEach(){
        verifiedAddressUtils = new VerifiedAddressUtils(addressBookDao);
        verificationCodeUtils = new VerificationCodeUtils(addressBookDao, pnUserattributesConfig, pnDatavaultClient, pnExternalChannelClient, verifiedAddressUtils);
        appIOUtils = new AppIOUtils(addressBookDao, verifiedAddressUtils, ioFunctionServicesClient, ioNotificationService);
        addressBookService = new AddressBookService(addressBookDao, pnDatavaultClient, courtesyDigitalAddressToDto, legalDigitalAddressToDto, pnSelfcareClient, verificationCodeUtils, appIOUtils);
    }


    @Test
    void saveLegalAddressBook() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @Test
    void saveLegalAddressBookWithVerificationCode() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), addressVerificationDto.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setPecValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto)
                .block(d);

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
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationCodeMaxAttempts()).thenReturn(3);
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);

        //THEN
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(d));

    }

    @Test
    void saveLegalAddressBookWithInvalidVerificationCodeTooLate() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), addressVerificationDto.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setPecValid(true);
        verificationCode.setLastModified(Instant.now().minus(1000, ChronoUnit.MINUTES));
        verificationCode.setCreated(Instant.now().minus(1000, ChronoUnit.MINUTES));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        assertThrows(PnExpiredVerificationCodeException.class, () -> mono.block(d));

        verificationCode.setLastModified(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setCreated(Instant.now().minus(1, ChronoUnit.SECONDS));

        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));


        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono_ok = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        assertDoesNotThrow(() -> mono_ok.block(d));

        //THEN
    }


    @Test
    void saveLegalAddressBookWithVerificationCodeRequestId() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String uuid = UUID.randomUUID().toString();
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setRequestId(uuid);
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setPecValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto)
                .block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS, result);
    }

    @Test
    void saveLegalAddressBookWithInvalidVerificationCodePecNotValid() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setLastModified(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setCreated(Instant.now().minus(1, ChronoUnit.SECONDS));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono_ok = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        AddressBookService.SAVE_ADDRESS_RESULT res = mono_ok.block(d);

        //THEN
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.PEC_VALIDATION_REQUIRED, res);
    }

    @Test
    void saveLegalAddressBookWithInvalidVerificationCodeTooLateLastModified() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setLastModified(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setCreated(Instant.now().minus(1000, ChronoUnit.SECONDS));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationCodeLegalTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono_ok = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        assertDoesNotThrow(() -> mono_ok.block(d));

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

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto).block(d);

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

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @Test
    void saveCourtesyAddressBookEmail_removeprevious() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "asasd", CourtesyChannelTypeDto.EMAIL.getValue(), "default", CourtesyAddressTypeDto.COURTESY.getValue(), "prova@prova.it");
        verificationCode.setVerificationCode("12345");

        VerificationCodeEntity verificationCodeOLD = new VerificationCodeEntity(recipientId, "asasd", CourtesyChannelTypeDto.EMAIL.getValue(), senderId, CourtesyAddressTypeDto.COURTESY.getValue(), "prova1@prova.it");
        verificationCodeOLD.setVerificationCode("12346");
        List<VerificationCodeEntity> listPrevious = new ArrayList<>();
        listPrevious.add(verificationCodeOLD);

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.fromIterable(listPrevious));
        Mockito.when(addressBookDao.deleteVerificationCode(verificationCodeOLD)).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
        verify(addressBookDao).deleteVerificationCode(verificationCodeOLD);
    }


    @Test
    void saveCourtesyAddressBookEmail_removeprevious_skipped() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "asasd", CourtesyChannelTypeDto.EMAIL.getValue(), "default", CourtesyAddressTypeDto.COURTESY.getValue(), "prova@prova.it");
        verificationCode.setVerificationCode("12345");

        VerificationCodeEntity verificationCodeOLD = new VerificationCodeEntity(recipientId, "asasd", CourtesyChannelTypeDto.SMS.getValue(), senderId, CourtesyAddressTypeDto.COURTESY.getValue(), "333333333");
        verificationCodeOLD.setVerificationCode("12346");
        List<VerificationCodeEntity> listPrevious = new ArrayList<>();
        listPrevious.add(verificationCodeOLD);

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.fromIterable(listPrevious));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
        verify(addressBookDao, never()).deleteVerificationCode(verificationCodeOLD);
    }

    @Test
    void saveCourtesyAddressBookSMS() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("+393331234567");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, null, courtesyChannelType, addressVerificationDto, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }


    @Test
    void saveCourtesyAddressBookSMS_fail() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("+383331234567");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        // WHEN
        assertThrows(PnInvalidInputException.class, () -> {
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);
        });
    }

    @Test
    void saveCourtesyAddressBookSMS_invalid() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("123345345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        // WHEN
        assertThrows(PnInvalidInputException.class, () -> {
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);
        });

        //THEN
    }


    @Test
    void saveCourtesyAddressBookEMAIL_invalid() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("abcd");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        // WHEN
        assertThrows(PnInvalidInputException.class, () ->
        {
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);
        });

        //THEN
    }



    @Test
    void saveCourtesyAddressBookEMAIL_invalid_evil() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("somethingverylong@hereandthereseemore-.com");

        // questo indirizzo crea problemi con alcune regex per check dellel email
        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        // WHEN
        assertThrows(PnInvalidInputException.class, () -> {
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);
        });

        //THEN
    }


    @Test
    void saveCourtesyAddressBookPEC_invalid() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelTypeDto = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("abcd");

        // WHEN
        assertThrows(PnInvalidInputException.class, () -> {
            addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelTypeDto, addressVerificationDto);
        });

        //THEN
    }



    @Test
    void saveCourtesyAddressBookAPPIO() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioNotificationService.scheduleCheckNotificationToSendAfterIOActivation(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, new AddressVerificationDto()).block(d);

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

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.error(new RuntimeException()));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, new AddressVerificationDto());
        assertThrows(RuntimeException.class, () -> mono.block(d));

        //THEN
    }

    @Test
    void saveCourtesyAddressBookAPPIO_FAIL2() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        AddressBookEntity addressBook = new AddressBookEntity();
        addressBook.setAddresshash(AddressBookEntity.APP_IO_DISABLED);

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.just(addressBook));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(false));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, new AddressVerificationDto());
        assertThrows(PnInternalException.class, () -> mono.block(d));

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

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", courtesyChannelType.getValue(), senderId, CourtesyAddressTypeDto.COURTESY.getValue(), addressVerificationDto.getValue());
        verificationCode.setLastModified(Instant.now().minusSeconds(1));
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getVerificationCodeCourtesyTTL()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

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

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", courtesyChannelType.getValue());
        verificationCode.setLastModified(Instant.now().minusSeconds(1));
        verificationCode.setVerificationCode("555555");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationCodeCourtesyTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationCodeMaxAttempts()).thenReturn(3);
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);

        //THEN
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(d));

    }


    @Test
    void saveCourtesyAddressBookWithInvalidVerificationCodeRetryLimit() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", courtesyChannelType.getValue());
        verificationCode.setLastModified(Instant.now().minusSeconds(1));
        verificationCode.setVerificationCode("555555");
        verificationCode.setFailedAttempts(2);

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationCodeCourtesyTTL()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationCodeMaxAttempts()).thenReturn(3);
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto);

        //THEN
        assertThrows(PnRetryLimitVerificationCodeException.class, () -> mono.block(d));

    }

    @Test
    void saveCourtesyAddressBookWithAlreadyVerified() {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", courtesyChannelType.getValue());
        verificationCode.setLastModified(Instant.now().minusSeconds(1));
        verificationCode.setVerificationCode("12345");
        verificationCode.setFailedAttempts(2);

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

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
        Object result = addressBookService.deleteLegalAddressBook(recipientId, null, legalChannelType, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

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
        Object result = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

        //THEN
        assertNotNull( result );
    }


    @Test
    void deleteCourtesyAddressBookAPPIO() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.just(new AddressBookEntity()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(false));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Object result = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType).block(d);

        //THEN
        assertNotNull( result );
    }


    @Test
    void deleteCourtesyAddressBookAPPIO_FAIL() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.just(new AddressBookEntity()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.error(new RuntimeException()));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Object> mono = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType);

        //THEN
        assertThrows(RuntimeException.class, () -> mono.block(d));

    }

    @Test
    void deleteCourtesyAddressBookAPPIO_FAIL2() {
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.just(new AddressBookEntity()));
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Object> mono = addressBookService.deleteCourtesyAddressBook(recipientId, null, courtesyChannelType);

        //THEN
        assertThrows(PnInternalException.class, () -> mono.block(d));

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
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);


        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAddresses(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(),listFromDb.get(0).getSenderId()).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(2, result.size());
            Assertions.assertNotNull(result.get(0).getSenderId());
            Assertions.assertTrue(result.contains(result.get(0)));
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void getCourtesyAddressByRecipientAndSenderWithAPPIO() {
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
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);


        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.PN_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAddresses(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(),listFromDb.get(0).getSenderId()).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(3, result.size());
            Assertions.assertNotNull(result.get(0).getSenderId());
            Assertions.assertTrue(result.contains(result.get(0)));
        } catch (Exception e) {
            fail(e);
        }
    }




    @Test
    void getCourtesyAddressByRecipientAndSender_PG() {
        //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true, null, "PEC", false));
        listFromDb.add(AddressBookDaoTestIT.newAddress(false, null, "EMAIL", false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(1).getAddressId(), dto);

        final CourtesyDigitalAddressDto resdto1 = new CourtesyDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);


        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.PN_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAddresses(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(),listFromDb.get(0).getSenderId()).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(2, result.size());
            Assertions.assertNotNull(result.get(0).getSenderId());
            Assertions.assertTrue(result.contains(result.get(0)));
        } catch (Exception e) {
            fail(e);
        }

        verify(ioFunctionServicesClient, never()).checkValidUsers(Mockito.any());
    }

    @Test
    void isAppIoEnabledByRecipient(){
        //Given
        AddressBookEntity addressBook = AddressBookDaoTestIT.newAddress(true, null, "APPIO", true);
        addressBook.setAddresshash(AddressBookEntity.APP_IO_ENABLED);


        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(List.of(addressBook)));

        //When
        Boolean result = addressBookService.isAppIoEnabledByRecipient(addressBook.getRecipientId()).block(d);

        //Then
        assertNotNull(result);
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
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null)
                .collectList()
                .block(d);

        //Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }




    @Test
    void getCourtesyAddressByRecipientWithAppIo() {
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
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.PN_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipient(listFromDb.get(0).getRecipientId(), null, null, null)
                .collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void getCourtesyAddressByRecipientWithAppIoERROR() {
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
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.EMAIL);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(CourtesyChannelTypeDto.SMS);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.ERROR);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        Mono<List<CourtesyDigitalAddressDto>> addressBookServiceMono = addressBookService.getCourtesyAddressByRecipient(listFromDb.get(0).getRecipientId(), null, null, null)
                .collectList();

        //Then
        assertThrows(PnInternalException.class, () -> addressBookServiceMono.block(d));
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
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setChannelType(LegalChannelTypeDto.PEC);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
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
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.APPIO);


        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());

        //When
        List<LegalAndUnverifiedDigitalAddressDto> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(2, result.size());


    }


    @Test
    void getLegalAddressByRecipient_WithVcs() {
       //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true));
        listFromDb.add(AddressBookDaoTestIT.newAddress(false));
        String recipientId = "";

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(1).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        recipientId = listFromDb.get(0).getRecipientId();
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.APPIO);



        final List<VerificationCodeEntity> listVcs = new ArrayList<>();

        VerificationCodeEntity verificationCode1 = new VerificationCodeEntity(recipientId, "hashed1", LegalChannelTypeDto.PEC.getValue(), "senderId1", LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode1.setLastModified(Instant.now().minusSeconds(1));
        verificationCode1.setVerificationCode("12345");
        verificationCode1.setCodeValid(true);
        verificationCode1.setPecValid(false);
        verificationCode1.setRequestId(UUID.randomUUID().toString());

        VerificationCodeEntity verificationCode2 = new VerificationCodeEntity(recipientId, "hashed2", LegalChannelTypeDto.PEC.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec1@pec.it");
        verificationCode2.setLastModified(Instant.now().minusSeconds(1));
        verificationCode2.setVerificationCode("54321");
        verificationCode2.setCodeValid(false);
        verificationCode2.setPecValid(true);
        verificationCode2.setRequestId(UUID.randomUUID().toString());

        listVcs.add(verificationCode1);
        listVcs.add(verificationCode2);

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.fromIterable(listVcs));





        //When
        List<LegalAndUnverifiedDigitalAddressDto> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(4, result.size());
        for (LegalAndUnverifiedDigitalAddressDto c :
                result) {
            for (VerificationCodeEntity vc :
                    listVcs) {
                if (c.getSenderId() != null && c.getSenderId().equals(listVcs.get(0).getSenderId()))
                {
                    assertEquals(listVcs.get(0).isCodeValid(), c.getCodeValid());
                    assertEquals(listVcs.get(0).isPecValid(), c.getPecValid());
                    assertNull(c.getValue());
                    assertNotNull(c.getRequestId());
                    assertEquals(listVcs.get(0).getRequestId(), c.getRequestId());
                }
            }
        }

    }


    @Test
    void getLegalAddressByRecipient_WithVcsOnly() {
        //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true));
        listFromDb.add(AddressBookDaoTestIT.newAddress(false));
        String recipientId = "";

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(1).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        recipientId = listFromDb.get(0).getRecipientId();
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDb.get(1).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.APPIO);



        final List<VerificationCodeEntity> listVcs = new ArrayList<>();

        VerificationCodeEntity verificationCode1 = new VerificationCodeEntity(recipientId, "hashed1", LegalChannelTypeDto.PEC.getValue(), "senderId1", LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode1.setLastModified(Instant.now().minusSeconds(1));
        verificationCode1.setVerificationCode("12345");
        verificationCode1.setCodeValid(true);
        verificationCode1.setPecValid(false);
        verificationCode1.setRequestId(UUID.randomUUID().toString());

        VerificationCodeEntity verificationCode2 = new VerificationCodeEntity(recipientId, "hashed2", LegalChannelTypeDto.PEC.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec1@pec.it");
        verificationCode2.setLastModified(Instant.now().minusSeconds(1));
        verificationCode2.setVerificationCode("54321");
        verificationCode2.setCodeValid(false);
        verificationCode2.setPecValid(true);
        verificationCode2.setRequestId(UUID.randomUUID().toString());

        listVcs.add(verificationCode1);
        listVcs.add(verificationCode2);

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.fromIterable(listVcs));


        //When
        List<LegalAndUnverifiedDigitalAddressDto> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList().block(d);

        //Then
        assertNotNull(result);
        assertEquals(2, result.size());
        for (LegalAndUnverifiedDigitalAddressDto c :
                result) {
            for (VerificationCodeEntity vc :
                    listVcs) {
                if (c.getSenderId() != null && c.getSenderId().equals(listVcs.get(0).getSenderId()))
                {
                    assertEquals(listVcs.get(0).isCodeValid(), c.getCodeValid());
                    assertEquals(listVcs.get(0).isPecValid(), c.getPecValid());
                    assertNull(c.getValue());
                    assertNotNull(c.getRequestId());
                    assertEquals(listVcs.get(0).getRequestId(), c.getRequestId());
                }
            }
        }

    }

    @Test
    void getAddressesByRecipient() {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true, "abc"));

        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);

        final CourtesyDigitalAddressDto resdto2 = new CourtesyDigitalAddressDto();
        resdto2.setRecipientId(listFromDbCourtesy.get(0).getRecipientId());
        resdto2.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto2.setSenderId(listFromDbCourtesy.get(0).getSenderId());
        resdto2.setChannelType(CourtesyChannelTypeDto.EMAIL);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());

        final List<PaSummary> paSummaries = new ArrayList<>();
        PaSummary paSummary = new PaSummary();
        paSummary.setId(resdto1.getSenderId());
        paSummary.setName("Fake pa");
        paSummaries.add(paSummary);

        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.fromIterable(listFromDbLegal));
        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.fromIterable(listFromDbCourtesy));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto2);
        when(pnSelfcareClient.getManyPaByIds(Mockito.any())).thenReturn(Flux.fromIterable(paSummaries));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(1, result.getLegal().size());
        assertEquals("Fake pa", result.getLegal().get(0).getSenderName());
        assertEquals(1, result.getCourtesy().size());
        assertNull(result.getCourtesy().get(0).getSenderName());
    }


    @Test
    void getAddressesByRecipient_noPAName() {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true, "abc"));

        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);

        final CourtesyDigitalAddressDto resdto2 = new CourtesyDigitalAddressDto();
        resdto2.setRecipientId(listFromDbCourtesy.get(0).getRecipientId());
        resdto2.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto2.setSenderId(listFromDbCourtesy.get(0).getSenderId());
        resdto2.setChannelType(CourtesyChannelTypeDto.EMAIL);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());


        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.fromIterable(listFromDbLegal));
        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.fromIterable(listFromDbCourtesy));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto2);
        when(pnSelfcareClient.getManyPaByIds(Mockito.any())).thenReturn(Flux.empty());
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(1, result.getLegal().size());
        assertNull(result.getLegal().get(0).getSenderName());
        assertEquals(1, result.getCourtesy().size());
        assertNull(result.getCourtesy().get(0).getSenderName());
    }



    @Test
    void getAddressesByRecipient_defaultOnly() {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true));

        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(LegalChannelTypeDto.PEC);

        final CourtesyDigitalAddressDto resdto2 = new CourtesyDigitalAddressDto();
        resdto2.setRecipientId(listFromDbCourtesy.get(0).getRecipientId());
        resdto2.setAddressType(CourtesyAddressTypeDto.COURTESY);
        resdto2.setSenderId(listFromDbCourtesy.get(0).getSenderId());
        resdto2.setChannelType(CourtesyChannelTypeDto.EMAIL);

        final BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("utente test");
        baseRecipientDtoDto.setRecipientType(RecipientTypeDto.PF);
        baseRecipientDtoDto.setInternalId("123456");

        final UserStatusResponse user = new UserStatusResponse();
        user.setStatus(UserStatusResponse.StatusEnum.APPIO_NOT_ACTIVE);
        user.setTaxId(baseRecipientDtoDto.getTaxId());


        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.fromIterable(listFromDbLegal));
        when(addressBookDao.getAllAddressesByRecipient (Mockito.any(), Mockito.eq(CourtesyAddressTypeDto.COURTESY.getValue()))).thenReturn(Flux.fromIterable(listFromDbCourtesy));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto2);
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(1, result.getLegal().size());
        assertNull(result.getLegal().get(0).getSenderName());
        assertEquals(1, result.getCourtesy().size());
        assertNull(result.getCourtesy().get(0).getSenderName());
    }
}