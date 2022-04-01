package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(controllers = {CourtesyAddressController.class})
class CourtesyAddressControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CHANNELTYPE = "SMS";

    @Autowired
    WebTestClient webTestClient;

    @Test
    void postRecipientCourtesyAddress() {
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy/{senderId}/{channelType}"
                    .replace("{recipientId}", RECIPIENTID)
                    .replace("{senderId}", SENDERID)
                    .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(addressVerification), AddressVerificationDto.class)
                .header(PA_ID)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void deleteRecipientCourtesyAddress() {
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy/{senderId}/{channelType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);


        webTestClient.delete()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void getCourtesyAddressByRecipient() {
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy"
                .replace("{recipientId}", RECIPIENTID);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCourtesyAddressBySender() {
        String url = "/address-book-private/v1/digital-address/courtesy/{recipientId}/{senderId}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }
}