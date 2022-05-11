package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.InternalErrorException;
import it.pagopa.pn.user.attributes.exceptions.InvalidChannelErrorException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesyMailRequestDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalCourtesySmsRequestDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.DigitalNotificationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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


    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
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


    public Mono<Void> sendVerificationCode(String address, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        String requestId = UUID.randomUUID().toString();
        log.info("sending verification code address:{} vercode: {} channel:{} requestId:{}", address, verificationCode, legalChannelType!=null?legalChannelType.getValue():courtesyChannelType.getValue(), requestId);
        if (legalChannelType != null)
           return sendLegalVerificationCode(requestId, address, legalChannelType, verificationCode);
        else
            return sendCourtesyVerificationCode(requestId, address, courtesyChannelType, verificationCode);
    }

    private Mono<Void> sendLegalVerificationCode(String requestId, String address, LegalChannelTypeDto legalChannelType, String verificationCode)
    {
        if (legalChannelType != LegalChannelTypeDto.PEC)
            throw new InvalidChannelErrorException();

        DigitalNotificationRequestDto digitalNotificationRequestDto = new DigitalNotificationRequestDto();
        digitalNotificationRequestDto.setChannel(DigitalNotificationRequestDto.ChannelEnum.PEC);
        digitalNotificationRequestDto.setRequestId(requestId);
        digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
        digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequestDto.MessageContentTypeEnum.PLAIN);
        digitalNotificationRequestDto.setQos(DigitalNotificationRequestDto.QosEnum.INTERACTIVE);
        digitalNotificationRequestDto.setMessageText(getMailVerificationCodeBody(verificationCode, true));
        digitalNotificationRequestDto.setReceiverDigitalAddress(address);
        digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
        return digitalLegalMessagesApi
                .sendDigitalLegalMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );
    }

    private Mono<Void> sendCourtesyVerificationCode(String requestId, String address, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        if (courtesyChannelType == CourtesyChannelTypeDto.SMS)
        {
            DigitalCourtesySmsRequestDto digitalNotificationRequestDto = new DigitalCourtesySmsRequestDto();
            digitalNotificationRequestDto.setChannel(DigitalCourtesySmsRequestDto.ChannelEnum.SMS);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
            digitalNotificationRequestDto.setQos(DigitalCourtesySmsRequestDto.QosEnum.INTERACTIVE);
            digitalNotificationRequestDto.setMessageText(getMailVerificationCodeBody(verificationCode, false));
            digitalNotificationRequestDto.setReceiverDigitalAddress(address);
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            return digitalCourtesyMessagesApi
                    .sendCourtesyShortMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofMillis(25))
                                    .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                    );
        }
        else  if (courtesyChannelType == CourtesyChannelTypeDto.EMAIL)
        {
            DigitalCourtesyMailRequestDto digitalNotificationRequestDto = new DigitalCourtesyMailRequestDto();
            digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequestDto.ChannelEnum.EMAIL);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_VERIFICATION_CODE);
            digitalNotificationRequestDto.setQos(DigitalCourtesyMailRequestDto.QosEnum.INTERACTIVE);
            digitalNotificationRequestDto.setMessageText(getMailVerificationCodeBody(verificationCode, true));
            digitalNotificationRequestDto.setReceiverDigitalAddress(address);
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            return digitalCourtesyMessagesApi
                    .sendDigitalCourtesyMessage(requestId, pnUserattributesConfig.getClientExternalchannelsHeaderExtchCxId(), digitalNotificationRequestDto)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofMillis(25))
                                    .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                    );
        }
        else
            throw new InvalidChannelErrorException();
    }



    private String getMailVerificationCodeBody(String verificationCode, boolean isForEmail)
    {
        String message = isForEmail?pnUserattributesConfig.getVerificationCodeMessageEMAIL():pnUserattributesConfig.getVerificationCodeMessageSMS();
        message = String.format(message, verificationCode);
        return  message;
    }

}
