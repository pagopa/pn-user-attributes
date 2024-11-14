package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {ConsentsController.class})
class ConsentsControllerTest {
    private static final String PA_ID = "x-pagopa-pn-uid";
    private static final String CX_ID = "x-pagopa-pn-cx-id";
    private static final String ROLE = "x-pagopa-pn-cx-role";
    private static final String PA_CX_TYPE = "x-pagopa-pn-cx-type";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONSENTTYPE = "TOS";
    private static final String CONSENTYPE_SERCQ = ConsentTypeDto.TOS_SERCQ.getValue();
    private static final String CONSENTYPE_DATA_PRIVACY_SERCQ = ConsentTypeDto.DATAPRIVACY_SERCQ.getValue();
    private static final String CX_TYPE = "PF";
    private static final String PG_CX_TYPE = "PG";
    private static final String VERSION = "VERS1";

    @Autowired
    WebTestClient webTestClient;
    @MockBean
    private ConsentsService svc;
    @MockBean
    private PnUserattributesConfig config;

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
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
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
    void consentAction_disabled() {
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));
        when(config.isSercqEnabled()).thenReturn(false);

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
    void consentAction_SERCQ(){
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTYPE_SERCQ, null);
        ce.setAccepted(true);

        // When
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));
        when(config.isSercqEnabled()).thenReturn(true);

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
    void consentAction_SERCQ_DATAPRIVACY(){
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_DATA_PRIVACY_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));
        when(config.isSercqEnabled()).thenReturn(true);

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
    void consentAction_SERCQ_disabled(){
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));
        when(config.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void consentAction_SERCQ_DATAPRIVACY_disabled(){
        // Given
        String url = "/user-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_DATA_PRIVACY_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new Object()));
        when(config.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(PA_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isBadRequest();
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
        when(svc.consentAction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
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
        when(svc.getConsentByType(RECIPIENTID, CxTypeAuthFleetDto.PF, ConsentTypeDto.TOS, null))
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
        when(svc.getConsents(RECIPIENTID, CxTypeAuthFleetDto.PF))
                .thenReturn(Flux.just(consentDto));

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
    void getConsents_SERCQ() {
        String url = "/user-consents/v1/consents";

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setConsentType(ConsentTypeDto.TOS_SERCQ);

        // When
        when(svc.getConsents(RECIPIENTID, CxTypeAuthFleetDto.PF))
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
        when(svc.getConsents(RECIPIENTID, CxTypeAuthFleetDto.PF))
                .thenReturn(Flux.empty());

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
    void setPgConsentAction() {
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isOk();

    }

    @Test
    void setPgConsentAction_disabled(){
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(config.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void setPgConsentAction_SERCQ(){
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(config.isSercqEnabled()).thenReturn(true);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void setPgConsentAction_SERCQ_disabled(){
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(config.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void setPgConsentAction_SERCQ_DATAPRIVACY(){
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_DATA_PRIVACY_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(config.isSercqEnabled()).thenReturn(true);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void setPgConsentAction_SERCQ_DATAPRIVACY_disabled(){
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTYPE_DATA_PRIVACY_SERCQ)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(config.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void setPgConsentAction_FAIL() {
        // Given
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", CONSENTTYPE)
                .replace("{version}", VERSION);

        ConsentActionDto consentAction = new ConsentActionDto();
        consentAction.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(RECIPIENTID, CONSENTTYPE, null);
        ce.setAccepted(true);

        // When
        when(svc.setPgConsentAction(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(consentAction)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .header(ROLE, "ADMIN")
                .exchange()
                .expectStatus().is5xxServerError();

    }

    @Test
    void getPgConsentType() {
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
        .replace("{consentType}", ConsentTypeDto.TOS_DEST_B2B.getValue())
                .replace("{version}", VERSION);

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setIsFirstAccept(true);
        consentDto.setConsentVersion(VERSION);
        consentDto.setConsentType(ConsentTypeDto.TOS_DEST_B2B);

        // When
        when(svc.getPgConsentByType(any(), any(), any(),
                        any()))
                .thenReturn(Mono.just(consentDto));

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, PG_CX_TYPE)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getPgConsentType_ErrorTest() {
        String url = "/pg-consents/v1/consents/{consentType}?version={version}"
                .replace("{consentType}", ConsentTypeDto.TOS_DEST_B2B.getValue())
                .replace("{version}", VERSION);

        // Given
        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(RECIPIENTID);
        consentDto.setAccepted(true);
        consentDto.setIsFirstAccept(true);
        consentDto.setConsentVersion(VERSION);
        consentDto.setConsentType(ConsentTypeDto.TOS_DEST_B2B);

        // When
        when(svc.getPgConsentByType(any(), any(), any(),
                        any()))
                .thenReturn(Mono.error(new PnForbiddenException()));

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(CX_ID, RECIPIENTID)
                .header(PA_CX_TYPE, CX_TYPE)
                .exchange()
                .expectStatus().isForbidden();
    }
}