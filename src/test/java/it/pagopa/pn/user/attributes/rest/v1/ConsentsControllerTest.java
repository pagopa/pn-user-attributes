package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentActionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(controllers = {ConsentsController.class})
class ConsentsControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CONSENTTYPE = "TOS";

    @Autowired
    WebTestClient webTestClient;

    @Test
    void consentAction() {
        //

        String url = "/user-consents/v1/consents/{recipientId}/{consentType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{consentType}", CONSENTTYPE);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(consentAction), ConsentActionDto.class)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsentByType() {
        String url = "/user-consents/v1/consents/{recipientId}/{consentType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{consentType}", CONSENTTYPE);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsents() {
        String url = "/user-consents/v1/consents/{recipientId}"
                .replace("{recipientId}", RECIPIENTID);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }
}