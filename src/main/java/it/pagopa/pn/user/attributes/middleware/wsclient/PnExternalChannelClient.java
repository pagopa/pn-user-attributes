package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidInputException;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.DigitalCourtesyMailRequestDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.DigitalCourtesySmsRequestDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.DigitalNotificationRequestDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.commons.pnclients.CommonBaseClient.elabExceptionMessage;
import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_COURTESY_CHANNEL;
import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_LEGAL_CHANNEL;

/**
 * Classe wrapper di pn-external-channels, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnExternalChannelClient {

    public static final String EVENT_TYPE_VERIFICATION_CODE = "VerificationCode";
    private final PnUserattributesConfig pnUserattributesConfig;
    private final DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private final DigitalLegalMessagesApi digitalLegalMessagesApi;
    private final PnDataVaultClient dataVaultClient;
    private final TemplateGenerator templateGenerator;
    private static final String PF_PREFIX = "PF";
    private static final String PG_PREFIX = "PG";

    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig,
                                   DigitalCourtesyMessagesApi digitalCourtesyMessagesApi, DigitalLegalMessagesApi digitalLegalMessagesApi, PnDataVaultClient dataVaultClient,
                                   TemplateGenerator templateGenerator) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.digitalCourtesyMessagesApi = digitalCourtesyMessagesApi;
        this.digitalLegalMessagesApi = digitalLegalMessagesApi;
        this.dataVaultClient = dataVaultClient;
        this.templateGenerator = templateGenerator;
    }




    public Mono<String> sendCourtesyPecRejected(String requestId, String recipientId, String address)
    {
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, "Sending PEC rejected", requestId);

        if ( ! pnUserattributesConfig.isDevelopment() ) {
            String logMessage = String.format(
                    "sendCourtesyPecRejected EMAIL sending pec address rejected recipient=%s address=%s channel=%s requestId=%s",
                    recipientId, LogUtils.maskEmailAddress(address), DigitalCourtesyMailRequestDto.ChannelEnum.EMAIL, requestId
            );
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, logMessage)
                    .build();
            logEvent.log();
            return sendCourtesyEmail(recipientId, requestId, DigitalCourtesyMailRequestDto.QosEnum.BATCH, templateGenerator.generatePecRejectBody(getRecipientType(recipientId)),
                    templateGenerator.generatePecSubjectReject(), address)
                    .onErrorResume(x -> {
                        String message = elabExceptionMessage(x);

                        String failureMessage = String.format("sendCourtesyPecRejected EMAIL response error %s", message);
                        logEvent.generateFailure(failureMessage).log();
                        log.error("sendCourtesyPecRejected EMAIL response error {}", message, x);
                        return Mono.error(x);
                    })
                    .then(Mono.fromSupplier(
                            () -> {
                                logEvent.generateWarning(logMessage).log(); // non genero il success, visto che la pec non era valida
                                return requestId;
                            }
                    ));
        }
        else {
            log.warn("DEVELOPMENT IS ACTIVE, MOCKING MESSAGE SEND REJECTED!!!!");
            log.warn("recipientId={} address={}",
                    recipientId, address);
            return Mono.just(requestId);
        }
    }

    public Mono<String> sendPecConfirm(String requestId, String recipientId, String address)
    {
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, "Sending PEC confirm", requestId);

        if ( ! pnUserattributesConfig.isDevelopment() ) {
            return sendLegalMessage(recipientId, requestId, address, LegalChannelTypeDto.PEC,
                    templateGenerator.generatePecConfirmBody(getRecipientType(recipientId)), templateGenerator.generatePecSubjectConfirm());
        }
        else {
            log.warn("DEVELOPMENT IS ACTIVE, MOCKING MESSAGE SEND CONFIRM!!!!");
            log.warn("recipientId={} address={}",
                    recipientId, address);
            return Mono.just(requestId);
        }
    }

    public Mono<String> sendVerificationCode(String recipientId, String address, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        String requestId = UUID.randomUUID().toString();
        if ( ! pnUserattributesConfig.isDevelopment() ) {
            if (legalChannelType != null)
                return sendLegalVerificationCode(recipientId, requestId, address, legalChannelType, verificationCode);
            else
                return sendCourtesyVerificationCode(recipientId, requestId, address, courtesyChannelType, verificationCode);
        }
        else {
            log.warn("DEVELOPMENT IS ACTIVE, MOCKING MESSAGE SEND!!!!");
            log.warn("recipientId={} address={} legalChannelType={} courtesyChannelType={} verificationCode={}",
                    recipientId, address, legalChannelType, courtesyChannelType, verificationCode);
            return Mono.just(requestId);
        }
    }

    private Mono<String> sendLegalVerificationCode(String recipientId, String requestId, String address, LegalChannelTypeDto legalChannelType, String verificationCode)
    {
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, "Sending legal verification code", requestId);
        String logMessage = String.format(
                "sendLegalVerificationCode PEC sending verification code recipientId=%s address=%s vercode=%s channel=%s requestId=%s",
                recipientId, LogUtils.maskEmailAddress(address), verificationCode, legalChannelType.getValue(), requestId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_VERIFY_PEC, logMessage)
                .build();
        logEvent.log();


        return sendLegalMessage(recipientId, requestId, address, legalChannelType, templateGenerator.generatePecBody(verificationCode), templateGenerator.generatePecSubject())
                .onErrorResume(x -> {
                    String message = elabExceptionMessage(x);
                    String failureMessage = String.format("sendLegalVerificationCode PEC response error %s", message);
                    logEvent.generateFailure(failureMessage).log();
                    log.error("sendLegalVerificationCode PEC response error {}", message, x);
                    return Mono.error(x);
                })
                .then(Mono.fromSupplier(
                        () -> {
                            logEvent.generateSuccess(logMessage).log();
                            return requestId;
                        }
                ));
    }


    private Mono<String> sendLegalMessage(String recipientId, String requestId, String address, LegalChannelTypeDto legalChannelType, String body, String subject)
    {
        if (legalChannelType != LegalChannelTypeDto.PEC)
            throw new PnInvalidInputException(ERROR_CODE_INVALID_LEGAL_CHANNEL, "legalChannelType");

        return dataVaultClient.getRecipientDenominationByInternalId(List.of(recipientId))
                .map(recipientDtoDto -> {
                    DigitalNotificationRequestDto digitalNotificationRequestDto = new DigitalNotificationRequestDto();
                    digitalNotificationRequestDto.setChannel(DigitalNotificationRequestDto.ChannelEnum.PEC);
                    digitalNotificationRequestDto.setRequestId(requestId);
                    digitalNotificationRequestDto.setCorrelationId(requestId);
                    digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
                    digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequestDto.MessageContentTypeEnum.HTML);
                    digitalNotificationRequestDto.setQos(DigitalNotificationRequestDto.QosEnum.INTERACTIVE);
                    digitalNotificationRequestDto.setMessageText(body);
                    digitalNotificationRequestDto.setReceiverDigitalAddress(address);
                    digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
                    digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
                    digitalNotificationRequestDto.setSubjectText(subject);
                    if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderPec()))
                        digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderPec());

                    return  digitalNotificationRequestDto;
                })
                .take(1)
                .next()
                .flatMap(digitalNotificationRequestDto -> digitalLegalMessagesApi
                        .sendDigitalLegalMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                )
                .then(Mono.just(requestId));
    }

    private Mono<String> sendCourtesyVerificationCode(String recipientId, String requestId, String address, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, "Sending courtesy verification code", requestId);
        if (courtesyChannelType == CourtesyChannelTypeDto.SMS)
        {
            String logMessage = String.format(
                    "sendCourtesyVerificationCode SMS sending verification code recipientId=%s address=%s vercode=%s channel=%s requestId=%s",
                    recipientId, LogUtils.maskNumber(address), verificationCode, courtesyChannelType.getValue(), requestId
            );
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_AB_VERIFY_SMS, logMessage)
                    .build();
            logEvent.log();

            DigitalCourtesySmsRequestDto digitalNotificationRequestDto = new DigitalCourtesySmsRequestDto();
            digitalNotificationRequestDto.setChannel(DigitalCourtesySmsRequestDto.ChannelEnum.SMS);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
            digitalNotificationRequestDto.setQos(DigitalCourtesySmsRequestDto.QosEnum.INTERACTIVE);
            digitalNotificationRequestDto.setMessageText(getSMSVerificationCodeBody(verificationCode));
            digitalNotificationRequestDto.setReceiverDigitalAddress(address);
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderSms()))
                digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderSms());

            return digitalCourtesyMessagesApi
                    .sendCourtesyShortMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                    .onErrorResume(x -> {
                        String message = elabExceptionMessage(x);
                        String failureMessage = String.format("sendCourtesyVerificationCode SMS response error %s", message);
                        logEvent.generateFailure(failureMessage).log();
                        log.error("sendCourtesyVerificationCode SMS response error {}", message, x);
                        return Mono.error(x);
                    })
                    .then(Mono.fromSupplier(
                            () -> {
                                logEvent.generateSuccess(logMessage).log();
                                return requestId;
                            }
                    ));
        }
        else  if (courtesyChannelType == CourtesyChannelTypeDto.EMAIL)
        {
            String logMessage = String.format(
                    "sendCourtesyVerificationCode EMAIL sending verification code recipientId=%s address=%s vercode=%s channel=%s requestId=%s",
                    recipientId, LogUtils.maskNumber(address), verificationCode, courtesyChannelType.getValue(), requestId
            );
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_AB_VERIFY_MAIL, logMessage)
                    .build();
            logEvent.log();
            return sendCourtesyEmail(recipientId, requestId, DigitalCourtesyMailRequestDto.QosEnum.INTERACTIVE, templateGenerator.generateEmailBody(verificationCode,getRecipientType(recipientId)),
                    templateGenerator.generateEmailSubject(), address)
                    .onErrorResume(x -> {
                        String message = elabExceptionMessage(x);

                        String failureMessage = String.format("sendCourtesyVerificationCode EMAIL response error %s", message);
                        logEvent.generateFailure(failureMessage).log();
                        log.error("sendCourtesyVerificationCode EMAIL response error {}", message, x);
                        return Mono.error(x);
                    })
                    .then(Mono.fromSupplier(
                            () -> {
                                logEvent.generateSuccess(logMessage).log();
                                return requestId;
                            }
                    ));

        }
        else
            throw new PnInvalidInputException(ERROR_CODE_INVALID_COURTESY_CHANNEL, "courtesyChannelType");
    }

    @NotNull
    private Mono<Void> sendCourtesyEmail(String recipientId, String requestId, DigitalCourtesyMailRequestDto.QosEnum batch, String messageBody, String messageSubject, String address) {
        return dataVaultClient.getRecipientDenominationByInternalId(List.of(recipientId))
                .map(recipientDtoDto -> {
                    DigitalCourtesyMailRequestDto digitalNotificationRequestDto = new DigitalCourtesyMailRequestDto();
                    digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequestDto.ChannelEnum.EMAIL);
                    digitalNotificationRequestDto.setRequestId(requestId);
                    digitalNotificationRequestDto.setCorrelationId(requestId);
                    digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
                    digitalNotificationRequestDto.setQos(batch);
                    digitalNotificationRequestDto.setMessageText(messageBody);
                    digitalNotificationRequestDto.setReceiverDigitalAddress(address);
                    digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
                    digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
                    digitalNotificationRequestDto.setSubjectText(messageSubject);
                    digitalNotificationRequestDto.setMessageContentType(DigitalCourtesyMailRequestDto.MessageContentTypeEnum.HTML);
                    if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderEmail()))
                        digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderEmail());

                    return digitalNotificationRequestDto;
                })
                .take(1)
                .next()
                .flatMap(digitalNotificationRequestDto -> digitalCourtesyMessagesApi
                        .sendDigitalCourtesyMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                );
    }

    private String getSMSVerificationCodeBody(String verificationCode)
    {
        String message = templateGenerator.generateSmsBody();
        message = String.format(message, verificationCode);
        return  message;
    }

    private String getRecipientType(String recipientId) {
        //default empty string if the value cannot be retrieved
        String result = "";
        if(recipientId != null){
            if(recipientId.startsWith(PF_PREFIX)) result = "PF";
            if(recipientId.startsWith(PG_PREFIX)) result = "PG";
        }
        log.info("getRecipientType result: {} , for recipientId: {}", result, recipientId);
        return result;
    }
}
