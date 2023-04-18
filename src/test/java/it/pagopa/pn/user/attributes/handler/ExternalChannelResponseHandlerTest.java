package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.services.utils.VerifiedAddressUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


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
        this.externalChannelResponseHandler = new ExternalChannelResponseHandler(pnUserattributesConfig, addressBookDao, verificationCodeUtils, pnExternalChannelClient);
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

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalchannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(Mockito.anyString(), Mockito.anyString());
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

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getExternalchannelDigitalCodesSuccess()).thenReturn(List.of("C003"));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(Mockito.anyString(), Mockito.anyString());
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

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalchannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.error(new NullPointerException()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertThrows(NullPointerException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(Mockito.anyString(), Mockito.anyString());
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

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalchannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(Mockito.any())).thenReturn(Mono.error(ConditionalCheckFailedException.builder().build()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(Mockito.anyString(), Mockito.anyString());
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

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(Mockito.any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalchannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendPecConfirm(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(UUID.randomUUID().toString()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.atMostOnce()).sendPecConfirm(Mockito.anyString(), Mockito.anyString());
    }
}