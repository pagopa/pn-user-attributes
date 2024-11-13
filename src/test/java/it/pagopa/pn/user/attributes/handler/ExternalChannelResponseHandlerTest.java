package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.services.utils.VerifiedAddressUtils;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.CourtesyMessageProgressEventDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExternalChannelResponseHandlerTest {

    private final Duration d = Duration.ofMillis(3000);

    private ExternalChannelResponseHandler externalChannelResponseHandler;

    @Mock
    PnUserattributesConfig pnUserattributesConfig;

    @Mock
    AddressBookDao addressBookDao;

    @Mock
    AddressBookService addressBookService;

    @Mock
    PnExternalChannelClient pnExternalChannelClient;

    @Mock
    PnDataVaultClient pnDatavaultClient;

    VerificationCodeUtils verificationCodeUtils;

    VerifiedAddressUtils verifiedAddressUtils;

    @BeforeEach
    public void before(){
        MockitoAnnotations.openMocks(pnExternalChannelClient);
        MockitoAnnotations.openMocks(pnDatavaultClient);
        verifiedAddressUtils = new VerifiedAddressUtils(addressBookDao);
        verificationCodeUtils = new VerificationCodeUtils(addressBookDao, pnUserattributesConfig, pnDatavaultClient, pnExternalChannelClient, verifiedAddressUtils);
        this.externalChannelResponseHandler = new ExternalChannelResponseHandler(pnUserattributesConfig, addressBookService, addressBookDao, verificationCodeUtils, pnExternalChannelClient, pnDatavaultClient);
    }

    @Test
    void consumeExternalChannelResponse_nocodevalid() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(false);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }


    @Test
    void consumeExternalChannelResponse_notfound() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(false);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }


    @Test
    void consumeExternalChannelResponse_found_butfail() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(false);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(any())).thenReturn(Mono.error(new NullPointerException()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertThrows(NullPointerException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }



    @Test
    void consumeExternalChannelResponse_found_butfail_ConditionalCheckFailedException() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(false);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(any())).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }

    @Test
    void consumeExternalChannelResponse_codevalid() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendPecConfirm(anyString(), anyString(), anyString())).thenReturn(Mono.just(UUID.randomUUID().toString()));
        Mockito.when(addressBookService.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.just(new LegalDigitalAddressDto().senderId("senderId").recipientId("recipientId").channelType(LegalChannelTypeDto.PEC)));
        Mockito.when(addressBookService.prepareAndDeleteAddresses(any())).thenReturn(Mono.just(List.of()));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto().value("value")));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.atMostOnce()).sendPecConfirm(anyString(), anyString(), anyString());
    }



    @Test
    void consumeExternalChannelResponse_progress() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C004");


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }


    @Test
    void consumeExternalChannelResponse_notlegal() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalCourtesy(new CourtesyMessageProgressEventDto());
        singleStatusUpdateDto.getDigitalCourtesy().setRequestId(requestId);


        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setValue("prova@prova.it");

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));


        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString());
    }
}