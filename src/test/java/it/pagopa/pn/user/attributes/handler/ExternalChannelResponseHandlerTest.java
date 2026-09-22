package it.pagopa.pn.user.attributes.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLog;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
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
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

    private Logger auditLogger;
    private ListAppender<ILoggingEvent> auditAppender;
    private Level auditPreviousLevel;

    @BeforeEach
    public void before(){
        MockitoAnnotations.openMocks(pnExternalChannelClient);
        MockitoAnnotations.openMocks(pnDatavaultClient);
        verifiedAddressUtils = new VerifiedAddressUtils(addressBookDao);
        verificationCodeUtils = new VerificationCodeUtils(addressBookDao, pnUserattributesConfig, pnDatavaultClient, pnExternalChannelClient, verifiedAddressUtils);
        this.externalChannelResponseHandler = new ExternalChannelResponseHandler(pnUserattributesConfig, addressBookService, addressBookDao, verificationCodeUtils, pnExternalChannelClient, pnDatavaultClient);
        MDC.clear();
        auditLogger = (Logger) LoggerFactory.getLogger(PnAuditLog.class);
        auditPreviousLevel = auditLogger.getLevel();
        auditLogger.setLevel(Level.INFO);
        auditAppender = new ListAppender<>();
        auditAppender.start();
        auditLogger.addAppender(auditAppender);
    }

    @AfterEach
    public void after(){
        auditLogger.detachAppender(auditAppender);
        auditAppender.stop();
        auditLogger.setLevel(auditPreviousLevel);
        MDC.clear();
    }

    private List<ILoggingEvent> auditRows(PnAuditLogEventType audType, Level level) {
        return auditAppender.list.stream()
                .filter(e -> audType.toString().equals(e.getMDCPropertyMap().get("aud_type")))
                .filter(e -> level == null || level.equals(e.getLevel()))
                .toList();
    }

    private String auditCxId(ILoggingEvent event) {
        return event.getMDCPropertyMap().get(MDCUtils.MDC_CX_ID_KEY);
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
        AtomicReference<String> cxIdInContext = new AtomicReference<>();
        Mockito.when(addressBookDao.updateVerificationCodeIfExists(any()))
                .thenReturn(Mono.deferContextual(ctx -> {
                    cxIdInContext.set(ctx.getOrDefault(MDCUtils.MDC_CX_ID_KEY, null));
                    return Mono.empty();
                }));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        Assertions.assertTrue(auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, null).stream().anyMatch(e -> recipientId.equals(auditCxId(e))));
        Assertions.assertEquals(recipientId, cxIdInContext.get());
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
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        List<ILoggingEvent> warnings = auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, Level.WARN);
        Assertions.assertEquals(1, warnings.size());
        Assertions.assertNull(auditCxId(warnings.get(0)));
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
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        List<ILoggingEvent> failures = auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, Level.ERROR);
        Assertions.assertEquals(1, failures.size());
        Assertions.assertEquals(recipientId, auditCxId(failures.get(0)));
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
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
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
        AtomicReference<String> cxIdInContext = new AtomicReference<>();
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(any(), any(), any()))
                .thenReturn(Mono.deferContextual(ctx -> {
                    cxIdInContext.set(ctx.getOrDefault(MDCUtils.MDC_CX_ID_KEY, null));
                    return Mono.empty();
                }));
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
        Mockito.when(addressBookService.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.just(new LegalDigitalAddressDto().senderId("senderId").recipientId("recipientId").channelType(LegalChannelTypeDto.PEC)));
        Mockito.when(addressBookService.prepareAndDeleteAddresses(any())).thenReturn(Mono.just(List.of()));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto().value("value")));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.atMostOnce()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        Assertions.assertTrue(auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, null).stream().anyMatch(e -> recipientId.equals(auditCxId(e))));
        Assertions.assertEquals(recipientId, cxIdInContext.get());
    }

    @Test
    void consumeExternalChannelResponse_codevalid_languageDE_propagatesDE() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C003");

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));
        verificationCode.setLanguage("DE");

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(addressBookDao.saveAddressBookAndVerifiedAddress(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(pnDatavaultClient.updateRecipientAddressByInternalId(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
        Mockito.when(addressBookService.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.just(new LegalDigitalAddressDto().senderId("senderId").recipientId("recipientId").channelType(LegalChannelTypeDto.PEC)));
        Mockito.when(addressBookService.prepareAndDeleteAddresses(any())).thenReturn(Mono.just(List.of()));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto().value("value")));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient).sendPecConfirm(anyString(), anyString(), anyString(), Mockito.eq(LanguageEnum.DE));
    }

    @Test
    void consumeExternalChannelResponse_vcAddressNotFound() {
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
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());
        Mockito.when(addressBookService.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.just(new LegalDigitalAddressDto().senderId("senderId").recipientId("recipientId").channelType(LegalChannelTypeDto.PEC)));
        Mockito.when(addressBookService.prepareAndDeleteAddresses(any())).thenReturn(Mono.just(List.of()));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto()));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertThrows(PnInternalException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
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
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesFail()).thenReturn(List.of("C009"));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendCourtesyPecRejected(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        Mockito.verify(addressBookDao, Mockito.never()).deleteVerificationCode(any());
    }

    @Test
    void consumeExternalChannelResponse_permanentFailure_codeValidTrue_sendsRejectionAndDeletes() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C009");

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLanguage("DE");
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesFail()).thenReturn(List.of("C009"));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto().value("pec@pec.it")));
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(anyString(), anyString(), anyString(), any(LanguageEnum.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
        AtomicReference<String> cxIdInContext = new AtomicReference<>();
        Mockito.when(addressBookDao.deleteVerificationCode(any()))
                .thenReturn(Mono.deferContextual(ctx -> {
                    cxIdInContext.set(ctx.getOrDefault(MDCUtils.MDC_CX_ID_KEY, null));
                    return Mono.empty();
                }));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.startsWith("pec-rejected-"), Mockito.eq(recipientId), Mockito.eq("pec@pec.it"), Mockito.eq(LanguageEnum.DE));
        Mockito.verify(addressBookDao).deleteVerificationCode(verificationCode);
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        Assertions.assertTrue(auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, null).stream().anyMatch(e -> recipientId.equals(auditCxId(e))));
        Assertions.assertEquals(recipientId, cxIdInContext.get());
    }

    @Test
    void consumeExternalChannelResponse_permanentFailure_codeValidFalse_stillSendsRejectionAndDeletes() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C009");

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(false);
        verificationCode.setLanguage("FR");
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesFail()).thenReturn(List.of("C009"));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.just(new AddressDtoDto().value("pec@pec.it")));
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(anyString(), anyString(), anyString(), any(LanguageEnum.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.startsWith("pec-rejected-"), Mockito.eq(recipientId), Mockito.eq("pec@pec.it"), Mockito.eq(LanguageEnum.FR));
        Mockito.verify(addressBookDao).deleteVerificationCode(verificationCode);
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
    }

    @Test
    void consumeExternalChannelResponse_permanentFailure_vcNotFound_noOp() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C009");

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.empty());
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesFail()).thenReturn(List.of("C009"));

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(addressBookDao, Mockito.never()).deleteVerificationCode(any());
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendCourtesyPecRejected(anyString(), anyString(), anyString(), any(LanguageEnum.class));
        List<ILoggingEvent> warnings = auditRows(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, Level.WARN);
        Assertions.assertEquals(1, warnings.size());
        Assertions.assertNull(auditCxId(warnings.get(0)));
    }

    @Test
    void consumeExternalChannelResponse_permanentFailure_languageNull_fallbackToIT() {
        //GIVEN
        String requestId = UUID.randomUUID().toString();
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setDigitalLegal(new LegalMessageSentDetailsDto());
        singleStatusUpdateDto.getDigitalLegal().setRequestId(requestId);
        singleStatusUpdateDto.getDigitalLegal().setEventCode("C009");

        String recipientId = "PF-123e4567-e89b-12d3-a456-426714174000";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(recipientId, "hashed", legalChannelType.getValue(), null, LegalAddressTypeDto.LEGAL.getValue(), "pec@pec.it");
        verificationCode.setVerificationCode("12345");
        verificationCode.setCodeValid(true);
        verificationCode.setLanguage(null);
        verificationCode.setLastModified(Instant.now().minusSeconds(1));

        Mockito.when(addressBookDao.getVerificationCodeByRequestId(any())).thenReturn(Mono.just(verificationCode));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesSuccess()).thenReturn(List.of("C003"));
        Mockito.when(pnUserattributesConfig.getExternalChannelDigitalCodesFail()).thenReturn(List.of("C009"));
        Mockito.when(pnDatavaultClient.getVerificationCodeAddressByInternalId(any(), any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(anyString(), anyString(), anyString(), any(LanguageEnum.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
        Mockito.when(addressBookDao.deleteVerificationCode(any())).thenReturn(Mono.empty());

        // WHEN
        Mono<Void> mono = externalChannelResponseHandler.consumeExternalChannelResponse(singleStatusUpdateDto);
        Assertions.assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.startsWith("pec-rejected-"), Mockito.eq(recipientId), Mockito.eq("pec@pec.it"), Mockito.eq(LanguageEnum.IT));
        Mockito.verify(addressBookDao).deleteVerificationCode(verificationCode);
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
        Mockito.verify(pnExternalChannelClient, Mockito.never()).sendPecConfirm(anyString(), anyString(), anyString(), any(LanguageEnum.class));
    }
}