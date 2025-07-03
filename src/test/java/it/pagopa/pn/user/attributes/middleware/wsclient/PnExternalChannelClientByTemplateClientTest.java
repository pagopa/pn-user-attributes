package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ExternalChannelHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_externalchannels_basepath=http://localhost:9998",
        "pn.env.runtime=PROD",
        "pn.user-attributes.enableTemplatesEngine=true"
})
class PnExternalChannelClientByTemplateClientTest {

    public static final String RECIPIENT_ID = "id-0d69-4ed6-a39f-4ef2f01f2fd1";
    public static final String ADDRESS = "realaddress@pec.it";
    public static final String DELIVERIES_COURTESY_FULL_MESSAGE_REQUESTS = "/external-channels/v1/digital-deliveries/courtesy-full-message-requests/.*";
    public static final String DELIVERIES_LEGAL_FULL_MESSAGE_REQUESTS = "/external-channels/v1/digital-deliveries/legal-full-message-requests/.*";
    public static final String COURTESY_SIMPLE_MESSAGE_REQUESTS = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/.*";

    @Autowired
    PnExternalChannelClient pnExternalChannelClient;

    @MockBean
    PnDataVaultClient pnDataVaultClient;

    @MockBean
    TemplateApi templateApi;

    @MockBean
    ActionHandler actionHandler;

    @MockBean
    SqsActionProducer sqsActionProducer;

    @MockBean
    ExternalChannelResponseHandler externalChannelResponseHandler;

    @MockBean
    ExternalChannelHandler externalChannelHandler;

    private static ClientAndServer mockServer;

    private final Duration duration = Duration.ofMillis(3000);

    @BeforeEach
    public void init() {
        mockServer = startClientAndServer(9998);
    }

    @AfterEach
    public void end() {
        stopQuietly(mockServer);
    }

    @Test
    void sendPECRejected() {
        //Given
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        // When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(templateApi.pecValidationContactsRejectBody(Mockito.any(),Mockito.any())).thenReturn("TEST- pecValidationContactsRejectBody");
        Mockito.when(templateApi.pecValidationContactsRejectSubject(Mockito.any())).thenReturn("TEST- pecValidationContactsRejectSubject");

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_COURTESY_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendCourtesyPecRejected("pec-rejected-1234567", RECIPIENT_ID, ADDRESS).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }

    @Test
    void sendPECRejectedException() {
        //Given
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.error(new Throwable("errore")));

        Mono<String> mono = pnExternalChannelClient.sendCourtesyPecRejected("pec-rejected-1234567", RECIPIENT_ID, ADDRESS);
        Assertions.assertThrows(Exception.class, () -> mono.block(Duration.ofMillis(3000)));
    }

    @Test
    void sendPECConfirm() {
        //Given

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        // When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(templateApi.pecValidationContactsSuccessBody(Mockito.any(),Mockito.any())).thenReturn("TEST- pecValidationContactsSuccessBody");
        Mockito.when(templateApi.pecValidationContactsSuccessSubject(Mockito.any())).thenReturn("TEST- pecValidationContactsSuccessSubject");

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_LEGAL_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendPecConfirm("pec-confirm-1234567", RECIPIENT_ID, ADDRESS).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }

    @Test
    void sendVerificationCodePEC() {
        //Given
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        CourtesyChannelTypeDto courtesyChannelType = null;
        String verificationCode = "12345";

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        // When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_LEGAL_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }

    @Test
    void sendVerificationCodePEC_FAIL() {
        //Given
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        CourtesyChannelTypeDto courtesyChannelType = null;
        String verificationCode = "12345";

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        //When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_LEGAL_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));
    }

    @Test
    void sendVerificationCodeEMAIL() {
        //Given
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        String verificationCode = "12345";
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        //When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_COURTESY_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }


    @Test
    void sendVerificationCodeEMAIL_FAIL() {
        //Given
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        String verificationCode = "12345";
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(RECIPIENT_ID);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        //When - Then
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(DELIVERIES_COURTESY_FULL_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));
    }

    @Test
    void sendVerificationCodeSMS() {
        //Given
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        String verificationCode = "12345";

        // When - Then
        Mockito.when(templateApi.smsVerificationCodeBody(Mockito.any())).thenReturn("TEST- smsVerificationCodeBody");
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(COURTESY_SIMPLE_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));
        String res = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }


    @Test
    void sendVerificationCodeSMS_FAIL() {
        //Given
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        String verificationCode = "12345";

        // When - Then
        Mockito.when(templateApi.smsVerificationCodeBody(Mockito.any())).thenReturn("TEST- smsVerificationCodeBody");
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(COURTESY_SIMPLE_MESSAGE_REQUESTS))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono = pnExternalChannelClient.sendVerificationCode(RECIPIENT_ID, ADDRESS, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));
    }
}
