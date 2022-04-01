package it.pagopa.pn.user.attributes.rest.v1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = {AllAddressController.class})
class AllAddressControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getAddressesByRecipient() {
        String url = "/address-book/v1/digital-address/{recipientId}"
                .replace("{recipientId}", RECIPIENTID);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }
}