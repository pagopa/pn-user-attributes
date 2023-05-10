package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidInputException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesyMailRequestDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesySmsRequestDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalNotificationRequestDto;
import it.pagopa.pn.user.attributes.utils.TemplateGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_COURTESY_CHANNEL;
import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_LEGAL_CHANNEL;

/**
 * Classe wrapper di pn-external-channels, con gestione del backoff
 */
@Component
@Slf4j
public class PnExternalChannelClient extends CommonBaseClient {

    public static final String EVENT_TYPE_VERIFICATION_CODE = "VerificationCode";
    private final PnUserattributesConfig pnUserattributesConfig;
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private DigitalLegalMessagesApi digitalLegalMessagesApi;
    private final PnDataVaultClient dataVaultClient;
    private final TemplateGenerator templateGenerator;

    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient dataVaultClient, TemplateGenerator templateGenerator) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.dataVaultClient = dataVaultClient;
        this.templateGenerator = templateGenerator;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalchannelsBasepath());

        this.digitalLegalMessagesApi = new DigitalLegalMessagesApi(apiClient);

        apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalchannelsBasepath());

        this.digitalCourtesyMessagesApi = new DigitalCourtesyMessagesApi(apiClient);
    }


    public Mono<String> sendPecConfirm(String recipientId, String address)
    {
        String requestId = UUID.randomUUID().toString();
        if ( ! pnUserattributesConfig.isDevelopment() ) {
            return sendLegalMessage(recipientId, requestId, address, LegalChannelTypeDto.PEC,
                    templateGenerator.generatePecConfirmBody(), pnUserattributesConfig.getVerificationCodeMessagePECConfirmSubject());
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
        String logMessage = String.format(
                "sendLegalVerificationCode PEC sending verification code recipientId=%s address=%s vercode=%s channel=%s requestId=%s",
                recipientId, LogUtils.maskEmailAddress(address), verificationCode, legalChannelType.getValue(), requestId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_VERIFY_PEC, logMessage)
                .build();
        logEvent.log();


        return sendLegalMessage(recipientId, requestId, address, legalChannelType, templateGenerator.generatePecBody(verificationCode), pnUserattributesConfig.getVerificationCodeMessagePECSubject())
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
            return dataVaultClient.getRecipientDenominationByInternalId(List.of(recipientId))
                    .map(recipientDtoDto -> {
                        DigitalCourtesyMailRequestDto digitalNotificationRequestDto = new DigitalCourtesyMailRequestDto();
                        digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequestDto.ChannelEnum.EMAIL);
                        digitalNotificationRequestDto.setRequestId(requestId);
                        digitalNotificationRequestDto.setCorrelationId(requestId);
                        digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
                        digitalNotificationRequestDto.setQos(DigitalCourtesyMailRequestDto.QosEnum.INTERACTIVE);
                        digitalNotificationRequestDto.setMessageText(templateGenerator.generateEmailBody(verificationCode));
                        digitalNotificationRequestDto.setReceiverDigitalAddress(address);
                        digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
                        digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
                        digitalNotificationRequestDto.setSubjectText(pnUserattributesConfig.getVerificationCodeMessageEMAILSubject());
                        digitalNotificationRequestDto.setMessageContentType(DigitalCourtesyMailRequestDto.MessageContentTypeEnum.HTML);
                        if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderEmail()))
                            digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderEmail());

                        return  digitalNotificationRequestDto;
                    })
                    .take(1)
                    .next()
                    .flatMap(digitalNotificationRequestDto -> digitalCourtesyMessagesApi
                            .sendDigitalCourtesyMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                            )
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


    private String getSMSVerificationCodeBody(String verificationCode)
    {
        String message = pnUserattributesConfig.getVerificationCodeMessageSMS();
        message = String.format(message, verificationCode);
        return  message;
    }
}
