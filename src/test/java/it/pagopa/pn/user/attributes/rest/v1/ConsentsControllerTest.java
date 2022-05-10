package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {ConsentsController.class})
class ConsentsControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONSENTTYPE = "TOS";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private ConsentsService svc;

    /**
     * Test commentato perchè da revisionare (genera una NullPointerException)
     */
    @Test
    void consentAction() {
        // Given
        String url = "/user-consents/v1/consents/{recipientId}/{consentType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{consentType}", CONSENTTYPE);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);
        Mono<ConsentActionDto> consentActionDtoMono = Mono.just(consentAction);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE);
        ce.setAccepted(true);

        // When
        Mockito.when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(consentActionDtoMono, ConsentActionDto.class)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsentByType() {
        // Given
        String url = "/user-consents/v1/consents/{recipientId}/{consentType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{consentType}", CONSENTTYPE);

        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsentByType(RECIPIENTID, ConsentTypeDto.TOS))
                .thenReturn( Mono.just(consentDto) );

        // Then
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

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsents(RECIPIENTID))
                .thenReturn( Flux.just(consentDto) );

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsents_NotFound() {
        String url = "/user-consents/v1/consents/{recipientId}"
                .replace("{recipientId}", RECIPIENTID);

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsents(RECIPIENTID))
                .thenReturn( Flux.empty() );

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isNotFound();
    }
}