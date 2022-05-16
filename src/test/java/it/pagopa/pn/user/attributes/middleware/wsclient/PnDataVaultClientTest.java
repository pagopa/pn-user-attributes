package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientTypeDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user.attributes.client_datavault_basepath=http://localhost:9998"
})
class PnDataVaultClientTest {
    @Autowired
    private PnDataVaultClient pnDataVaultClient;

    private static ClientAndServer mockServer;
    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void updateRecipientAddressByInternalId() {
        //Given
        String internalId ="id-0d69-4ed6-a39f-4ef2f01f2fd1";
        String addressId = "abcd-123-fghi";
        String realaddress ="realaddress";
        String name= "mario";
        String surname= "rossi";
        String ragionesociale= "mr srl";
        String path = "/datavault-private/v1/recipients/internal/{internalId}/addresses/{addressId}";
        path.replace("{internalId}",internalId);
        path.replace("{addressId}",addressId);


        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(204));

        //When
       Object result = pnDataVaultClient.updateRecipientAddressByInternalId(internalId, addressId,realaddress).block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals("OK", result);
    }



    @Test
    void getRecipientAddressesByInternalId() {
        //Given
        String internalId = "f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1";
        String address= "address";
        
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath("/datavault-private/v1/recipients/internal/{internalId}/addresses)".replace("{internalId}",internalId)))
                .respond(response()
                        .withBody("{" +
                                "\"" + RecipientAddressesDtoDto.JSON_PROPERTY_ADDRESSES + "\": " + "\"" + address + "\"," +
                                "}")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        RecipientAddressesDtoDto result = pnDataVaultClient.getRecipientAddressesByInternalId(internalId).block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(address, result.getAddresses());
    }


    @Test
    void deleteRecipientAddressByInternalId() {
    }

    @Test
    void getRecipientDenominationByInternalId() {
        //Given
        String cf = "RSSMRA85T10A562S";
        String iuid= "abcd-123-fghi";
        String denominazione = "mario rossi";
        List<String> list = new ArrayList<>();
        list.add(iuid);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withQueryStringParameter("internalId", iuid)
                        .withPath("/datavault-private/v1/recipients/internal"))
                .respond(response()
                        .withBody("{" +
                                "\"" + BaseRecipientDtoDto.JSON_PROPERTY_INTERNAL_ID + "\": " + "\"" + iuid + "\"," +
                                "\"" + BaseRecipientDtoDto.JSON_PROPERTY_DENOMINATION + "\": " + "\"" + denominazione + "\"" +
                                "}")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        List<BaseRecipientDtoDto> result = pnDataVaultClient.getRecipientDenominationByInternalId(list).collectList().block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(iuid, result.get(0).getInternalId());
        assertEquals(denominazione, result.get(0).getDenomination());
    }

}