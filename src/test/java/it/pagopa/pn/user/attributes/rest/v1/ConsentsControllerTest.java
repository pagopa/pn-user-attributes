package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import org.junit.jupiter.api.BeforeEach;
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
    private static final String PA_ID = "x-pagopa-pn-uid";
    private static final String PA_CX_TYPE = "x-pagopa-pn-cx-type";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONSENTTYPE = "TOS";
    private static final String CX_TYPE = "PF";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private ConsentsService svc;

    @MockBean
    PnAuditLogBuilder pnAuditLogBuilder;

    PnAuditLogEvent logEvent;

    @BeforeEach
    public void init(){
        logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(pnAuditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(pnAuditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(pnAuditLogBuilder);
        Mockito.when(pnAuditLogBuilder.uid(Mockito.anyString())).thenReturn(pnAuditLogBuilder);
        Mockito.when(logEvent.generateSuccess(Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.log()).thenReturn(logEvent);
    }

    @Test
    void consentAction() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}"
                .replace("{consentType}", CONSENTTYPE);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        Mockito.when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
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

        Mockito.verify(logEvent).generateSuccess(Mockito.any());
        Mockito.verify(logEvent, Mockito.never()).generateFailure(Mockito.any(), Mockito.any());
    }

    @Test
    void consentAction_FAIL() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}"
                .replace("{consentType}", CONSENTTYPE);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        Mockito.when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
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

        Mockito.verify(logEvent).generateFailure(Mockito.any(), Mockito.any());
        Mockito.verify(logEvent, Mockito.never()).generateSuccess(Mockito.any());
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
}