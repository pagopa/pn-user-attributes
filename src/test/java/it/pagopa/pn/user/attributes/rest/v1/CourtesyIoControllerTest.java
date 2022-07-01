package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;


@WebFluxTest(controllers = {CourtesyIoController.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "pn.user-attributes.client_io-activation-services_api-key=secret"
})
@SpringBootTest
class CourtesyIoControllerTest {

    private static final String HEADER_API_KEY = "Ocp-Apim-Subscription-Key";

    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;


    @MockBean
    PnAuditLogBuilder pnAuditLogBuilder;

    PnAuditLogEvent logEvent;

    @BeforeEach
    public void init(){
        logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(pnAuditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(pnAuditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(pnAuditLogBuilder);
        Mockito.when(logEvent.generateSuccess(Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.log()).thenReturn(logEvent);
    }

    @Test
    void getCourtesyAddressIo() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";


        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);


        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(HEADER_API_KEY, "secret")
                .header("x-pagopa-pn-cx-id", "")
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(logEvent).generateSuccess(Mockito.any());
        Mockito.verify(logEvent, Mockito.never()).generateFailure(Mockito.any(), Mockito.any());
    }

    @Test
    void setCourtesyAddressIo() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("12345");
        addressVerificationDto.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);


        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(HEADER_API_KEY, "secret")
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(logEvent).generateSuccess(Mockito.any());
        Mockito.verify(logEvent, Mockito.never()).generateFailure(Mockito.any(), Mockito.any());
    }
}