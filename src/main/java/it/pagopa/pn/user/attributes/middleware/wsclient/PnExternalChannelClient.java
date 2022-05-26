package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.InvalidChannelErrorException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.StringUtil;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesyMailRequestDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesySmsRequestDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalNotificationRequestDto;
import it.pagopa.pn.user.attributes.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe wrapper di pn-external-channels, con gestione del backoff
 */
@Component
@Slf4j
public class PnExternalChannelClient extends BaseClient {

    public static final String EVENT_TYPE_VERIFICATION_CODE = "VerificationCode";
    private final PnUserattributesConfig pnUserattributesConfig;
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private DigitalLegalMessagesApi digitalLegalMessagesApi;
    private final PnDataVaultClient dataVaultClient;


    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient dataVaultClient) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.dataVaultClient = dataVaultClient;
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


    public Mono<Void> sendVerificationCode(String recipientId, String address, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        if ( ! pnUserattributesConfig.isDevelopment() ) {
            String requestId = UUID.randomUUID().toString();
            if (legalChannelType != null)
                return sendLegalVerificationCode(recipientId, requestId, address, legalChannelType, verificationCode);
            else
                return sendCourtesyVerificationCode(recipientId, requestId, address, courtesyChannelType, verificationCode);
        }
        else {
            log.warn("DEVELOPMENT IS ACTIVE, MOCKING MESSAGE SEND!!!!");
            log.warn("recipientId={} address={} legalChannelType={} courtesyChannelType={} verificationCode={}",
                    recipientId, address, legalChannelType, courtesyChannelType, verificationCode);
            return Mono.empty();
        }
    }

    private Mono<Void> sendLegalVerificationCode(String recipientId, String requestId, String address, LegalChannelTypeDto legalChannelType, String verificationCode)
    {
        log.info("sendLegalVerificationCode PEC sending verification code address:{} vercode: {} channel:{} requestId:{}", LogUtils.maskEmailAddress(address), verificationCode, legalChannelType.getValue(), requestId);
        if (legalChannelType != LegalChannelTypeDto.PEC)
            throw new InvalidChannelErrorException();

        return dataVaultClient.getRecipientDenominationByInternalId(List.of(recipientId))
                .map(recipientDtoDto -> {
                    DigitalNotificationRequestDto digitalNotificationRequestDto = new DigitalNotificationRequestDto();
                    digitalNotificationRequestDto.setChannel(DigitalNotificationRequestDto.ChannelEnum.PEC);
                    digitalNotificationRequestDto.setRequestId(requestId);
                    digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
                    digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequestDto.MessageContentTypeEnum.PLAIN);
                    digitalNotificationRequestDto.setQos(DigitalNotificationRequestDto.QosEnum.INTERACTIVE);
                    digitalNotificationRequestDto.setMessageText(getMailVerificationCodeBody(verificationCode, recipientDtoDto.getDenomination()));
                    digitalNotificationRequestDto.setReceiverDigitalAddress(address);
                    digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
                    digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
                    digitalNotificationRequestDto.setSubjectText(pnUserattributesConfig.getVerificationCodeMessageEMAILSubject());
                    if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderPec()))
                        digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderPec());
                    return  digitalNotificationRequestDto;
                })
                .take(1)
                .next()
                .flatMap(digitalNotificationRequestDto -> digitalLegalMessagesApi
                        .sendDigitalLegalMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                        .retryWhen(
                                Retry.backoff(2, Duration.ofMillis(25))
                                        .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                        ))
                        .onErrorResume(WebClientResponseException.class, x -> {
                            log.error("sendLegalVerificationCode PEC response error {}", x.getResponseBodyAsString(), x);
                            return Mono.error(x);
                        });
    }

    private Mono<Void> sendCourtesyVerificationCode(String recipientId, String requestId, String address, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        if (courtesyChannelType == CourtesyChannelTypeDto.SMS)
        {
            log.info("sendCourtesyVerificationCode SMS sending verification code address:{} vercode: {} channel:{} requestId:{}", LogUtils.maskNumber(address), verificationCode, courtesyChannelType.getValue(), requestId);
            DigitalCourtesySmsRequestDto digitalNotificationRequestDto = new DigitalCourtesySmsRequestDto();
            digitalNotificationRequestDto.setChannel(DigitalCourtesySmsRequestDto.ChannelEnum.SMS);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
            digitalNotificationRequestDto.setQos(DigitalCourtesySmsRequestDto.QosEnum.INTERACTIVE);
            digitalNotificationRequestDto.setMessageText(getSMSVerificationCodeBody(verificationCode));
            digitalNotificationRequestDto.setReceiverDigitalAddress(address);
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderSms()))
                digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderSms());
            return digitalCourtesyMessagesApi
                    .sendCourtesyShortMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofMillis(25))
                                    .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                    )
                    .onErrorResume(WebClientResponseException.class, x -> {
                        log.error("sendCourtesyVerificationCode SMS response error {}", x.getResponseBodyAsString(), x);
                        return Mono.error(x);
                    });
        }
        else  if (courtesyChannelType == CourtesyChannelTypeDto.EMAIL)
        {
            log.info("sendCourtesyVerificationCode EMAIL sending verification code address:{} vercode: {} channel:{} requestId:{}", LogUtils.maskEmailAddress(address), verificationCode, courtesyChannelType.getValue(), requestId);
            return dataVaultClient.getRecipientDenominationByInternalId(List.of(recipientId))
                    .map(recipientDtoDto -> {
                        DigitalCourtesyMailRequestDto digitalNotificationRequestDto = new DigitalCourtesyMailRequestDto();
                        digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequestDto.ChannelEnum.EMAIL);
                        digitalNotificationRequestDto.setRequestId(requestId);
                        digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
                        digitalNotificationRequestDto.setQos(DigitalCourtesyMailRequestDto.QosEnum.INTERACTIVE);
                        digitalNotificationRequestDto.setMessageText(getMailVerificationCodeBody(verificationCode, recipientDtoDto.getDenomination()));
                        digitalNotificationRequestDto.setReceiverDigitalAddress(address);
                        digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
                        digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
                        digitalNotificationRequestDto.setSubjectText(pnUserattributesConfig.getVerificationCodeMessageEMAILSubject());
                        if (StringUtils.hasText(pnUserattributesConfig.getClientExternalchannelsSenderEmail()))
                            digitalNotificationRequestDto.setSenderDigitalAddress(pnUserattributesConfig.getClientExternalchannelsSenderEmail());
                        return  digitalNotificationRequestDto;
                    })
                    .take(1)
                    .next()
                    .flatMap(digitalNotificationRequestDto -> digitalCourtesyMessagesApi
                            .sendDigitalCourtesyMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                            .retryWhen(
                                    Retry.backoff(2, Duration.ofMillis(25))
                                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                            ))
                    .onErrorResume(WebClientResponseException.class, x -> {
                        log.error("sendCourtesyVerificationCode EMAIL response error {}", x.getResponseBodyAsString(), x);
                        return Mono.error(x);
                    });

        }
        else
            throw new InvalidChannelErrorException();
    }



    private String getMailVerificationCodeBody(String verificationCode, String nameSurname)
    {
        String message = pnUserattributesConfig.getVerificationCodeMessageEMAIL();
        message = String.format(message, nameSurname, verificationCode);
        return  message;
    }

    private String getSMSVerificationCodeBody(String verificationCode)
    {
        String message = pnUserattributesConfig.getVerificationCodeMessageSMS();
        message = String.format(message, verificationCode);
        return  message;
    }


}
