package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import org.junit.jupiter.api.AfterEach;
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


@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_externalchannels_basepath=http://localhost:9998",
        "pn.env.runtime=PROD"
})
class PnExternalChannelClientTest {

    @Autowired
    private PnExternalChannelClient pnExternalChannelClient;

    @MockBean
    PnDataVaultClient pnDataVaultClient;

    @MockBean
    ActionHandler actionHandler;

    @MockBean
    SqsActionProducer sqsActionProducer;

    private static ClientAndServer mockServer;

    private final Duration duration = Duration.ofMillis(3000);

    @BeforeEach
    public void init(){
        mockServer = startClientAndServer(9998);
    }

    @AfterEach
    public void end(){
        mockServer.stop();
    }

    @Test
    void sendVerificationCodePEC() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        CourtesyChannelTypeDto courtesyChannelType = null;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/legal-full-message-requests/.*";

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(recipientId);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);


        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));

        // When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }

    @Test
    void sendVerificationCodePEC_FAIL() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd2";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = LegalChannelTypeDto.PEC;
        CourtesyChannelTypeDto courtesyChannelType = null;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/legal-full-message-requests/.*";

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(recipientId);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);


        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));

        //When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono = pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));

    }

    @Test
    void sendVerificationCodeEMAIL() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/courtesy-full-message-requests/.*";
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(recipientId);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));

        //When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }


    @Test
    void sendVerificationCodeEMAIL_FAIL() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd2";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.EMAIL;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/courtesy-full-message-requests/.*";
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId(recipientId);
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);

        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));

        //When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono =  pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));

    }


    @Test
    void sendVerificationCodeSMS() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/.*";

        // When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        String res = pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode).block(Duration.ofMillis(3000));
        assertNotNull(res);
    }


    @Test
    void sendVerificationCodeSMS_FAIL() {
        //Given
        String recipientId ="id-0d69-4ed6-a39f-4ef2f01f2fd2";
        String address ="realaddress@pec.it";
        LegalChannelTypeDto legalChannelType = null;
        CourtesyChannelTypeDto courtesyChannelType = CourtesyChannelTypeDto.SMS;
        String verificationCode = "12345";
        String path = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/.*";

        // When - Then
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(500));

        Mono<String> pnExternalChannelClientMono =  pnExternalChannelClient.sendVerificationCode(recipientId, address, legalChannelType, courtesyChannelType, verificationCode);
        assertThrows(WebClientResponseException.class, () -> pnExternalChannelClientMono.block(duration));

    }
}