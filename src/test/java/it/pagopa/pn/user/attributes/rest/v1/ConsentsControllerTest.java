package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@WebFluxTest(controllers = {ConsentsController.class})
class ConsentsControllerTest {
    private static final String PA_ID = "x-pagopa-pn-uid";
    private static final String CX_ID = "x-pagopa-pn-cx-id";
    private static final String PA_CX_TYPE = "x-pagopa-pn-cx-type";
    private static final String CX_ROLE = "x-pagopa-pn-cx-role";
    private static final String CX_GROUPS = "x-pagopa-pn-cx-groups";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONSENTTYPE = "TOS";
    private static final String CX_TYPE = "PF";
    private static final String PG_CX_TYPE = "PG";
    private static final String VERSION = "VERS1";
    private static final String ROLE = "ADMIN";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private ConsentsService svc;

    @Test
    void consentAction() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        Mockito.when(svc.consentAction(Mockito.anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.just(new Object()));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isOk();

    }

    @Test
    void consentAction_FAIL() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        Mockito.when(svc.consentAction(Mockito.anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().is5xxServerError();

    }

    @Test
    void getConsentByType() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}"
                .replace("{consentType}", CONSENTTYPE);

        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsentByType(RECIPIENTID, CxTypeAuthFleetDto.PF, ConsentTypeDto.TOS, null))
                .thenReturn( Mono.just(consentDto) );

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsents() {
        String url = "/user-consents/v1/consents";

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsents(RECIPIENTID, CxTypeAuthFleetDto.PF))
                .thenReturn( Flux.just(consentDto) );

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getConsents_NotFound() {
        String url = "/user-consents/v1/consents";

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS);

        // When
        Mockito.when(svc.getConsents(RECIPIENTID, CxTypeAuthFleetDto.PF))
                .thenReturn( Flux.empty() );

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isOk().expectBodyList(ConsentDto.class).hasSize(0);
    }

    @Test
    void getPgConsentType() {
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
        .replace("{consentType}", ConsentTypeDto.TOS_DEST_B2B.getValue())
                .replace("{version}", VERSION);

        List<String> groupList = Collections.emptyList();

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setIsFirstAccept(true);
        consentDto.setConsentVersion(VERSION);
        consentDto.setConsentType(ConsentTypeDto.TOS_DEST_B2B);

        // When
        Mockito.when(svc.getPgConsentByType(any(), any(), any(),
                        any(), any(), any()))
                .thenReturn(Mono.just(consentDto));

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(CX_ROLE, ROLE)
                .header(CX_GROUPS, String.valueOf(groupList))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getPgConsentType_ErrorTest() {
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", ConsentTypeDto.TOS_DEST_B2B.getValue())
                .replace("{version}", VERSION);

        List<String> groupList = Collections.emptyList();

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setIsFirstAccept(true);
        consentDto.setConsentVersion(VERSION);
        consentDto.setConsentType(ConsentTypeDto.TOS_DEST_B2B);

        // When
        Mockito.when(svc.getPgConsentByType(any(), any(), any(),
                        any(), any(), any()))
                .thenReturn(Mono.error(new PnForbiddenException()));

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .header(CX_ROLE, ROLE)
                .header(CX_GROUPS, String.valueOf(groupList))
                .exchange()
                .expectStatus().isForbidden();
    }
}