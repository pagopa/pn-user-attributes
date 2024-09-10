package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnExpiredVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidInputException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnRetryLimitVerificationCodeException;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.*;
import it.pagopa.pn.user.attributes.services.utils.AppIOUtils;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.services.utils.VerifiedAddressUtils;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.RecipientTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.UserStatusResponse;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.dto.PaSummary;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_SENDERIDNOTROOT;
import static it.pagopa.pn.user.attributes.services.AddressBookService.SAVE_ADDRESS_RESULT.PEC_VALIDATION_REQUIRED;
import static it.pagopa.pn.user.attributes.services.AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AddressBookServiceTest {

    private static final String ADDRESS_SERCQ = "x-pagopa-pn-sercq:SEND-self:notification-already-delivered";
    private final Duration d = Duration.ofMillis(3000);


    private AddressBookService addressBookService;



    private VerificationCodeUtils verificationCodeUtils;

    private AppIOUtils appIOUtils;

    private VerifiedAddressUtils verifiedAddressUtils;

    @Mock
    PnExternalRegistryClient pnExternalRegistryClient;

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

    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:SEND-self:notification-already-delivered";
    private static final String LEGAL_ADDRESS = "email@pec.it";
    private static final String COURTESY_ADDRESS = "email@email.it";




    @BeforeEach
    void beforeEach() {
        verifiedAddressUtils = new VerifiedAddressUtils(addressBookDao);
        verificationCodeUtils = new VerificationCodeUtils(addressBookDao, pnUserattributesConfig, pnDatavaultClient, pnExternalChannelClient, verifiedAddressUtils);
        appIOUtils = new AppIOUtils(addressBookDao, verifiedAddressUtils, ioFunctionServicesClient, ioNotificationService);
        addressBookService = new AddressBookService(addressBookDao, pnDatavaultClient, courtesyDigitalAddressToDto, legalDigitalAddressToDto, pnSelfcareClient, verificationCodeUtils, appIOUtils
                , pnExternalRegistryClient, pnUserattributesConfig);
    }


    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBook(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.saveVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnExternalChannelClient.sendVerificationCode(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

        //THEN
        assertNotNull( result );
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @Test
    void saveLegalAddressBook_SERCQ() {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.SERCQ;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(ADDRESS_SERCQ);

        VerificationCodeEntity entity = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), null);
        entity.setPecValid(false);
        entity.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.lenient().when(pnExternalChannelClient.sendVerificationCode(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.any(), Mockito.eq(LegalAddressTypeDto.LEGAL.getValue()))).thenReturn(Flux.empty());
        Mockito.lenient().when(addressBookDao.saveVerificationCode(any())).thenReturn(Mono.empty());
        Mockito.lenient().when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(entity));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto, CxTypeAuthFleetDto.PF, null, null).block();

        //THEN
        assertNotNull(result);
        assertEquals(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED, result);
    }

    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBookWithVerificationCode(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto)
                .block(d);

        //THEN
        assertNotNull(result);
        assertEquals(SUCCESS, result);
    }

    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBookWithInvalidVerificationCode(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("55555");
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationcodemaxattempts()).thenReturn(3);
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);

        //THEN
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(d));

    }

    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBookWithInvalidVerificationCodeTooLate(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
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


    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypes")
    void saveLegalAddressBookWithVerificationCodeRequestId(LegalChannelTypeDto legalChannelType) {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String uuid = UUID.randomUUID().toString();
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, null, legalChannelType, addressVerificationDto)
                .block(d);

        //THEN
        assertNotNull(result);
        assertEquals(SUCCESS, result);
    }

    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0} with expected result: {1}")
    @MethodSource("provideLegalChannelTypesAndResults")
    void saveLegalAddressBookWithInvalidVerificationCodePecNotValid(LegalChannelTypeDto legalChannelType, AddressBookService.SAVE_ADDRESS_RESULT expectedResult, String address) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setLastModified(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setCreated(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setAddress("prova@prova.it");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.lenient().when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.lenient().when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.lenient().when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
        Mockito.lenient().when(pnUserattributesConfig.getVerificationcodelegalttl()).thenReturn(Duration.ofSeconds(100));
        Mockito.lenient().when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono_ok = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        AddressBookService.SAVE_ADDRESS_RESULT res = mono_ok.block(d);

        //THEN
        assertEquals(expectedResult, res);
    }

    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBookWithInvalidVerificationCodeTooLateLastModified(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);
        addressVerificationDto.setVerificationCode("12345");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity("recipientId", "hashed", legalChannelType.getValue());
        verificationCode.setVerificationCode("12345");
        verificationCode.setLastModified(Instant.now().minus(1, ChronoUnit.SECONDS));
        verificationCode.setCreated(Instant.now().minus(1000, ChronoUnit.SECONDS));
        verificationCode.setAddress("prova@prova.it");

        // WHEN
        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.NOT_EXISTS));
        Mockito.when(addressBookDao.getVerificationCode(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
        Mockito.lenient().when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());
        Mockito.lenient().when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.lenient().when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.lenient().when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        //THEN
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> mono_ok = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto);
        assertDoesNotThrow(() -> mono_ok.block(d));

    }



    @ParameterizedTest(name = "Test saveLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypesAndAddress")
    void saveLegalAddressBookWithAlreadyVerified(LegalChannelTypeDto legalChannelType, String address) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue(address);

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(addressBookDao.validateHashedAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelType, addressVerificationDto).block(d);

        //THEN
        assertNotNull(result);
        assertEquals(SUCCESS, result);
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
        assertNotNull(result);
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
        assertNotNull(result);
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
        assertNotNull(result);
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
        assertNotNull(result);
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
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block();
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
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block();
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
        PnInvalidInputException thrown = assertThrows(PnInvalidInputException.class, () ->
        {
            addressBookService.saveCourtesyAddressBook(recipientId, null, courtesyChannelType, addressVerificationDto).block();
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
            addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block();
        });

        //THEN
    }


    @ParameterizedTest(name = "Test saveCourtesyAddressBook with channelType {0}")
    //TODO SERCQ investigare fallimento, probabilmente logica esclusiva per PEC
    @MethodSource("provideLegalChannelTypes")
    void saveCourtesyAddressBookPEC_invalid(LegalChannelTypeDto legalChannelType) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        String senderId = null;
        LegalChannelTypeDto legalChannelTypeDto = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("abcd");

        // WHEN
        assertThrows(PnInvalidInputException.class, () -> {
            addressBookService.saveLegalAddressBook(recipientId, senderId, legalChannelTypeDto, addressVerificationDto).block();
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
        assertNotNull(result);
        assertEquals(SUCCESS, result);
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, senderId, courtesyChannelType, addressVerificationDto).block(d);

        //THEN
        assertNotNull(result);
        assertEquals(SUCCESS, result);
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationcodemaxattempts()).thenReturn(3);
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
        Mockito.when(pnUserattributesConfig.getVerificationcodettl()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pnUserattributesConfig.getValidationcodemaxattempts()).thenReturn(3);
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
        assertNotNull(result);
        assertEquals(SUCCESS, result);
    }


    @ParameterizedTest(name = "Test deleteLegalAddressBook with channelType {0}")
    @MethodSource("provideLegalChannelTypes")
    void deleteLegalAddressBook(LegalChannelTypeDto legalChannelType) {
        //GIVEN

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";


        Mockito.when(addressBookDao.deleteAddressBook(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(new Object()));
        Mockito.when(pnDatavaultClient.deleteRecipientAddressByInternalId(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        // WHEN
        Object result = addressBookService.deleteLegalAddressBook(recipientId, null, legalChannelType, CxTypeAuthFleetDto.PF, null, null)
                .block(d);

        //THEN
        assertNotNull(result);
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
        assertNotNull(result);
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
        assertNotNull(result);
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

        when(addressBookDao.getAddresses(Mockito.any(), Mockito.any(), Mockito.any(), anyBoolean())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(), listFromDb.get(0).getSenderId()).collectList().block(d);

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

        when(addressBookDao.getAddresses(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(), listFromDb.get(0).getSenderId()).collectList().block(d);

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

        when(addressBookDao.getAddresses(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        List<CourtesyDigitalAddressDto> result = addressBookService.getCourtesyAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(), listFromDb.get(0).getSenderId()).collectList().block(d);

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
    void isAppIoEnabledByRecipient() {
        //Given
        AddressBookEntity addressBook = AddressBookDaoTestIT.newAddress(true, null, "APPIO", true);
        addressBook.setAddresshash(AddressBookEntity.APP_IO_ENABLED);


        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(List.of(addressBook)));

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

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
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

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
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

        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(ioFunctionServicesClient.checkValidUsers(Mockito.any())).thenReturn(Mono.just(user));
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenReturn(resdto1);

        //When
        Mono<List<CourtesyDigitalAddressDto>> addressBookServiceMono = addressBookService.getCourtesyAddressByRecipient(listFromDb.get(0).getRecipientId(), null, null, null)
                .collectList();

        //Then
        assertThrows(PnInternalException.class, () -> addressBookServiceMono.block(d));
    }

    @ParameterizedTest(name = "Test getLegalAddressBook with legalChannelType {0} and address {1}, expected results: {2}")
    @MethodSource("provideLegalChannelTypesAndAddressAndResults")
    void getLegalAddressByRecipientAndSender(LegalChannelTypeDto legalChannelType, String address, int results) {
        //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true, "default", legalChannelType.getValue(), true));


        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);

        //When
        when(addressBookDao.getAddresses(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        lenient().when(pnUserattributesConfig.getSercqAddress()).thenReturn(SERCQ_ADDRESS);


        //Then
        Mono<List<LegalDigitalAddressDto>> result = addressBookService.getLegalAddressByRecipientAndSender(listFromDb.get(0).getRecipientId(), listFromDb.get(0).getSenderId()).collectList();
        StepVerifier.create(result)
                .assertNext(legalDigitalAddressDtos -> {
                    Assertions.assertNotNull(legalDigitalAddressDtos);
                    Assertions.assertEquals(results, legalDigitalAddressDtos.size());
                    if (results != 0) {
                        Assertions.assertEquals("default", legalDigitalAddressDtos.get(0).getSenderId());
                        Assertions.assertEquals(legalChannelType, legalDigitalAddressDtos.get(0).getChannelType());
                        Assertions.assertEquals(address, legalDigitalAddressDtos.get(0).getValue());
                    }
                })
                .expectComplete()
                .verify(d);
    }


    @ParameterizedTest(name = "Test getLegalByRecipient with legalChannelType {0} and address {1}, expected results: {2}")
    @MethodSource("provideLegalChannelTypesAndAddressAndResults")
    void getLegalAddressByRecipient(LegalChannelTypeDto legalChannelType, String address, int results) {
        //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true, "default", legalChannelType.getValue(), true));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);

        //Whenenv
        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        lenient().when(pnUserattributesConfig.getSercqAddress()).thenReturn(SERCQ_ADDRESS);

        //Then
        Mono<List<LegalAndUnverifiedDigitalAddressDto>> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList();

        StepVerifier.create(result)
                .assertNext(legalAndUnverifiedDigitalAddressDtos -> {
                    Assertions.assertNotNull(legalAndUnverifiedDigitalAddressDtos);
                    Assertions.assertEquals(results, legalAndUnverifiedDigitalAddressDtos.size());
                    if(results != 0) {
                        Assertions.assertEquals("default", legalAndUnverifiedDigitalAddressDtos.get(0).getSenderId());
                        Assertions.assertEquals(legalChannelType, legalAndUnverifiedDigitalAddressDtos.get(0).getChannelType());
                        Assertions.assertEquals(address, legalAndUnverifiedDigitalAddressDtos.get(0).getValue());
                    }
                })
                .expectComplete()
                .verify(d);
    }


    @ParameterizedTest(name = "Test getLegalAddressByRecipient with legalChannelType {0}")
    @MethodSource("provideLegalChannelTypes")
    void getLegalAddressByRecipient_WithVcs(LegalChannelTypeDto legalChannelType) {
        //Given
        List<AddressBookEntity> listFromDb = new ArrayList<>();
        listFromDb.add(AddressBookDaoTestIT.newAddress(true));
        String recipientId = "";

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue("email@pec.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDb.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDb.get(0).getRecipientId());
        recipientId = listFromDb.get(0).getRecipientId();
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(legalChannelType);


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

        //When
        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.fromIterable(listVcs));
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();

        //Then
        Mono<List<LegalAndUnverifiedDigitalAddressDto>> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList();

        StepVerifier.create(result)
                .assertNext(legalAndUnverifiedDigitalAddressDtos -> {
                    Assertions.assertNotNull(legalAndUnverifiedDigitalAddressDtos);
                    Assertions.assertEquals(3, legalAndUnverifiedDigitalAddressDtos.size());
                    for (LegalAndUnverifiedDigitalAddressDto c :
                            legalAndUnverifiedDigitalAddressDtos) {
                        for (VerificationCodeEntity vc :
                                listVcs) {
                            if (c.getSenderId() != null && c.getSenderId().equals(listVcs.get(0).getSenderId())) {
                                Assertions.assertEquals(listVcs.get(0).isCodeValid(), c.getCodeValid());
                                Assertions.assertEquals(listVcs.get(0).isPecValid(), c.getPecValid());
                                Assertions.assertNull(c.getValue());
                                Assertions.assertNotNull(c.getRequestId());
                                Assertions.assertEquals(listVcs.get(0).getRequestId(), c.getRequestId());
                            }
                        }
                    }
                })
                .expectComplete()
                .verify(d);
    }


    @ParameterizedTest(name = "Test getLegalAddressByRecipient with legalChannelType {0}")
    @MethodSource("provideLegalChannelTypes")
    void getLegalAddressByRecipient_WithVcsOnly(LegalChannelTypeDto legalChannelType) {
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
        resdto1.setSenderId(listFromDb.get(0).getSenderId());
        resdto1.setChannelType(legalChannelType);
        resdto1.setRecipientId(listFromDb.get(1).getRecipientId());



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

        //When
        when(addressBookDao.getAllAddressesByRecipient(Mockito.any(),Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getRecipientAddressesByInternalId(Mockito.any())).thenReturn(Mono.just(recipientAddressesDtoDto));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.fromIterable(listVcs));


        //Then
        Mono<List<LegalAndUnverifiedDigitalAddressDto>> result = addressBookService.getLegalAddressByRecipient(listFromDb.get(0).getRecipientId(), CxTypeAuthFleetDto.PF, null, null).collectList();

        StepVerifier.create(result)
                .assertNext(legalAndUnverifiedDigitalAddressDtos -> {
                    Assertions.assertNotNull(legalAndUnverifiedDigitalAddressDtos);
                    Assertions.assertEquals(2, legalAndUnverifiedDigitalAddressDtos.size());
                    for (LegalAndUnverifiedDigitalAddressDto c :
                            legalAndUnverifiedDigitalAddressDtos) {
                        for (VerificationCodeEntity vc :
                                listVcs) {
                            if (c.getSenderId() != null && c.getSenderId().equals(listVcs.get(0).getSenderId())) {
                                Assertions.assertEquals(listVcs.get(0).isCodeValid(), c.getCodeValid());
                                Assertions.assertEquals(listVcs.get(0).isPecValid(), c.getPecValid());
                                Assertions.assertNull(c.getValue());
                                Assertions.assertNotNull(c.getRequestId());
                                Assertions.assertEquals(listVcs.get(0).getRequestId(), c.getRequestId());
                            }
                        }
                    }
                })
                .expectComplete()
                .verify(d);

    }

    @ParameterizedTest(name = "Test getAddressesByRecipient with legalChannelType {0} and address {1}, expected results: {2}")
    @MethodSource("provideLegalChannelTypesAndAddressAndResults")
    void getAddressesByRecipient(LegalChannelTypeDto legalChannelType, String address, int results) {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true, "abc", legalChannelType.getValue(), true));

        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(legalChannelType);

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
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        lenient().when(pnSelfcareClient.getManyPaByIds(Mockito.any())).thenReturn(Flux.fromIterable(paSummaries));
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());
        lenient().when(pnUserattributesConfig.getSercqAddress()).thenReturn(SERCQ_ADDRESS);

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(results, result.getLegal().size());
        if(results != 0) {
            assertEquals("Fake pa", result.getLegal().get(0).getSenderName());
            assertEquals(1, result.getCourtesy().size());
            assertNull(result.getCourtesy().get(0).getSenderName());
        }
    }


    @ParameterizedTest(name = "Test getAddressesByRecipient with legalChannelType {0} and address {1}, expected results: {2}")
    @MethodSource("provideLegalChannelTypesAndAddressAndResults")
    void getAddressesByRecipient_noPAName(LegalChannelTypeDto legalChannelType, String address, int results) {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true, "default", legalChannelType.getValue(), true));


        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(legalChannelType);

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
        when(legalDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        when(courtesyDigitalAddressToDto.toDto(Mockito.any())).thenCallRealMethod();
        lenient().when(pnSelfcareClient.getManyPaByIds(Mockito.any())).thenReturn(Flux.empty());
        when(addressBookDao.getAllVerificationCodesByRecipient(Mockito.anyString(), Mockito.any())).thenReturn(Flux.empty());
        lenient().when(pnUserattributesConfig.getSercqAddress()).thenReturn(SERCQ_ADDRESS);

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(results, result.getLegal().size());
        if(results != 0) {
            assertNull(result.getLegal().get(0).getSenderName());
            assertEquals(1, result.getCourtesy().size());
            assertNull(result.getCourtesy().get(0).getSenderName());
        }
    }



    @ParameterizedTest(name = "Test getAddressesByRecipient with legalChannelType {0} and address {1}, expected results: {2}")
    @MethodSource("provideLegalChannelTypesAndAddressAndResults")
    void getAddressesByRecipient_defaultOnly(LegalChannelTypeDto legalChannelType, String address, int results) {
        //Given
        List<AddressBookEntity> listFromDbLegal = new ArrayList<>();
        listFromDbLegal.add(AddressBookDaoTestIT.newAddress(true, "default", legalChannelType.getValue(), true));

        List<AddressBookEntity> listFromDbCourtesy = new ArrayList<>();
        listFromDbCourtesy.add(AddressBookDaoTestIT.newAddress(false));

        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(listFromDbLegal.get(0).getAddressId(), dto);
        dto = new AddressDtoDto();
        dto.setValue("email@email.it");
        recipientAddressesDtoDto.putAddressesItem(listFromDbCourtesy.get(0).getAddressId(), dto);

        final LegalDigitalAddressDto resdto1 = new LegalDigitalAddressDto();
        resdto1.setRecipientId(listFromDbLegal.get(0).getRecipientId());
        resdto1.setAddressType(LegalAddressTypeDto.LEGAL);
        resdto1.setSenderId(listFromDbLegal.get(0).getSenderId());
        resdto1.setChannelType(legalChannelType);

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
        lenient().when(pnUserattributesConfig.getSercqAddress()).thenReturn(SERCQ_ADDRESS);

        //When
        UserAddressesDto result = addressBookService.getAddressesByRecipient(listFromDbCourtesy.get(0).getRecipientId(), null, null, null).block(d);

        //Then
        assertNotNull(result);
        assertEquals(results, result.getLegal().size());
        if(results != 0) {
            assertNull(result.getLegal().get(0).getSenderName());
            assertEquals(1, result.getCourtesy().size());
            assertNull(result.getCourtesy().get(0).getSenderName());
        }
    }

    @ParameterizedTest(name = "Test saveNotRootId with legalChannelType {0}")
    @MethodSource("provideLegalChannelTypes")
    void saveNotRootId(LegalChannelTypeDto legalChannelType) {
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setVerificationCode("12345");

        Mockito.when(pnExternalRegistryClient.getAooUoIdsApi(Arrays.asList("NOTROOT"))).thenReturn(Flux.just("NOTROOT"));

        PnInvalidInputException thrown = assertThrows(
                PnInvalidInputException.class,
                () -> addressBookService.saveLegalAddressBook(recipientId, "NOTROOT", legalChannelType, addressVerificationDto, CxTypeAuthFleetDto.PF, null, null).block(),
                "Expected saveLegalAddressBook() to throw, but it didn't"
        );

        List<String> errorCodes = new ArrayList<>();
        thrown.getProblem().getErrors().forEach(e -> errorCodes.add(e.getCode()));
        Assertions.assertTrue(errorCodes.contains(ERROR_CODE_USERATTRIBUTES_SENDERIDNOTROOT));
    }

    @Test
    void saveRootId() {
        final String ROOT_SENDER = "ROOTID";
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";

        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioNotificationService.scheduleCheckNotificationToSendAfterIOActivation(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalRegistryClient.getAooUoIdsApi(Arrays.asList(ROOT_SENDER))).thenReturn(Flux.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, ROOT_SENDER, courtesyChannelType, new AddressVerificationDto()).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(SUCCESS, result);
    }

    @Test
    void saveDefaultSender() {
        final String ROOT_SENDER = null;
        //GIVEN
        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";

        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.APPIO;

        Mockito.when(addressBookDao.getAddressBook(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioFunctionServicesClient.upsertServiceActivation(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(true));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(ioNotificationService.scheduleCheckNotificationToSendAfterIOActivation(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        AddressBookService.SAVE_ADDRESS_RESULT result = addressBookService.saveCourtesyAddressBook(recipientId, ROOT_SENDER, courtesyChannelType, new AddressVerificationDto()).block(d);

        //THEN
        assertNotNull( result );
        assertEquals(SUCCESS, result);
    }



    private static Stream<Arguments> provideLegalChannelTypes() {
        return Stream.of(
                Arguments.of(LegalChannelTypeDto.PEC),
                Arguments.of(LegalChannelTypeDto.APPIO),
                Arguments.of(LegalChannelTypeDto.SERCQ)
        );
    }

    private static Stream<Arguments> provideLegalChannelTypesAndResults() {
        return Stream.of(
                Arguments.of(LegalChannelTypeDto.PEC,PEC_VALIDATION_REQUIRED, LEGAL_ADDRESS),
                Arguments.of(LegalChannelTypeDto.APPIO,SUCCESS, COURTESY_ADDRESS),
                Arguments.of(LegalChannelTypeDto.SERCQ,SUCCESS, ADDRESS_SERCQ)
        );
    }

    private static Stream<Arguments> provideLegalChannelTypesAndAddressAndResults() {
        return Stream.of(
                Arguments.of(LegalChannelTypeDto.PEC,LEGAL_ADDRESS,1),
                Arguments.of(LegalChannelTypeDto.APPIO,COURTESY_ADDRESS,1),
                Arguments.of(LegalChannelTypeDto.SERCQ,SERCQ_ADDRESS,1)

        );
    }

    public static Stream<Arguments> provideLegalChannelTypesAndAddress() {
        return Stream.of(
                Arguments.of(LegalChannelTypeDto.PEC,LEGAL_ADDRESS),
                Arguments.of(LegalChannelTypeDto.APPIO,COURTESY_ADDRESS),
                Arguments.of(LegalChannelTypeDto.SERCQ,SERCQ_ADDRESS)

        );
    }
}