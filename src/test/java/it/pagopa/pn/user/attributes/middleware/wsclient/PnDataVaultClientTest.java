package it.pagopa.pn.user.attributes.middleware.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ExternalChannelHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.RecipientAddressesDtoDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_datavault_basepath=http://localhost:9998",
        "pn.env.runtime=PROD"
})
class PnDataVaultClientTest {
    @Autowired
    private PnDataVaultClient pnDataVaultClient;

    @MockBean
    ActionHandler actionHandler;

    @MockBean
    SqsActionProducer sqsActionProducer;

    @MockBean
    ExternalChannelResponseHandler externalChannelResponseHandler;

    @MockBean
    ExternalChannelHandler externalChannelHandler;

    private static ClientAndServer mockServer;
    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @AfterEach
    public void afterEach() {
        mockServer.reset();
    }


    @Test
    void updateRecipientAddressByInternalId() {
        //Given
        String internalId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String addressId = "abcd-123-fghi";
        String realaddress ="realaddress";
        String path = "/datavault-private/v1/recipients/internal/{internalId}/addresses/{addressId}"
                .replace("{internalId}",internalId)
                .replace("{addressId}",addressId);

        //Mockito.when(pnUserattributesConfig.getClientDatavaultBasepath()).thenReturn("http://localhost:9998");


        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));
        //When
        assertDoesNotThrow(() -> pnDataVaultClient.updateRecipientAddressByInternalId(internalId, addressId,realaddress).block(Duration.ofMillis(3000)));
    }



    @Test
    void getRecipientAddressesByInternalId() throws JsonProcessingException {
        //Given
        String internalId = "f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1";
        String address= "address@prova.it";
        RecipientAddressesDtoDto recipientAddressesDtoDto = new RecipientAddressesDtoDto();
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(address);
        recipientAddressesDtoDto.putAddressesItem(internalId, dto);
        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(recipientAddressesDtoDto);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath("/datavault-private/v1/recipients/internal/{internalId}/addresses".replace("{internalId}",internalId)))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        RecipientAddressesDtoDto result = pnDataVaultClient.getRecipientAddressesByInternalId(internalId).block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
    }


    @Test
    void deleteRecipientAddressByInternalId() {
        //Given
        String internalId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String addressId = "abcd-123-fghi";
        String path = "/datavault-private/v1/recipients/internal/{internalId}/addresses/{addressId}"
                .replace("{internalId}",internalId)
                .replace("{addressId}",addressId);


        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("DELETE")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        //When
        assertDoesNotThrow(() -> pnDataVaultClient.deleteRecipientAddressByInternalId(internalId, addressId).block(Duration.ofMillis(3000)));

    }

    @Test
    void getRecipientDenominationByInternalId() throws JsonProcessingException {
        //Given
        String iuid= "abcd-123-fghi";
        String denominazione = "mario rossi";
        List<String> list = new ArrayList<>();
        list.add(iuid);
        BaseRecipientDtoDto dto = new BaseRecipientDtoDto();
        dto.setDenomination(denominazione);
        dto.setInternalId(iuid);
        List<BaseRecipientDtoDto> listsrc = new ArrayList<>();
        listsrc.add(dto);

        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(listsrc);


        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withQueryStringParameter("internalId", iuid)
                        .withPath("/datavault-private/v1/recipients/internal"))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        List<BaseRecipientDtoDto> result = pnDataVaultClient.getRecipientDenominationByInternalId(list).collectList().block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(iuid, result.get(0).getInternalId());
        assertEquals(denominazione, result.get(0).getDenomination());
    }

    @Test
    void getVerificationCodeAddressByInternalId() throws JsonProcessingException {
        //Given
        String internalId = "id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String hashedAddress = "abcd-123-fghi";
        String realAddress = "test@pec.com";
        String path = "/datavault-private/v1/recipients/internal/{internalId}/addresses"
                .replace("{internalId}", internalId);

        RecipientAddressesDtoDto body = new RecipientAddressesDtoDto().addresses(Map.of("VC#" + hashedAddress, new AddressDtoDto().value(realAddress)));
        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(body);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withBody(respJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        Mono<AddressDtoDto> result = pnDataVaultClient.getVerificationCodeAddressByInternalId(internalId, hashedAddress);

        //Then
        StepVerifier.create(result).expectNextMatches(dto -> dto.getValue().equals(realAddress)).verifyComplete();
    }

    @Test
    void getVerificationCodeAddressByInternalId_AddressNotFound() throws JsonProcessingException {
        //Given
        String internalId = "id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String hashedAddress = "abcd-123-fghi";
        String path = "/datavault-private/v1/recipients/internal/{internalId}/addresses"
                .replace("{internalId}", internalId);

        RecipientAddressesDtoDto body = new RecipientAddressesDtoDto().addresses(Map.of());
        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(body);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withBody(respJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        Mono<AddressDtoDto> result = pnDataVaultClient.getVerificationCodeAddressByInternalId(internalId, hashedAddress);

        //Then
        StepVerifier.create(result).verifyComplete();
    }

}