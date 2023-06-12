package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.io.v1.dto.IoCourtesyDigitalAddressActivationDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
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
class CourtesyIoControllerTest {

    private static final String HEADER_API_KEY = "Ocp-Apim-Subscription-Key";
    private static final String HEADER_CX_ID = "x-pagopa-pn-cx-id";


    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getCourtesyAddressIo() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";


        // When
        Mono<Boolean> voidReturn  = Mono.just(true);
        Mockito.when(this.svc.isAppIoEnabledByRecipient(Mockito.anyString()))
                .thenReturn(voidReturn);


        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(HEADER_API_KEY, "secret")
                .header(HEADER_CX_ID, "abcd")
                .exchange()
                .expectStatus().isOk();

    }

    @Test
    void setCourtesyAddressIo() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";

        // When

        IoCourtesyDigitalAddressActivationDto ioCourtesyDigitalAddressActivationDto = new IoCourtesyDigitalAddressActivationDto();
        ioCourtesyDigitalAddressActivationDto.setActivationStatus(true);

        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ioCourtesyDigitalAddressActivationDto)
                .header(HEADER_API_KEY, "secret")
                .header(HEADER_CX_ID, "abcd")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void setCourtesyAddressIoDisable() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";

        // When

        IoCourtesyDigitalAddressActivationDto ioCourtesyDigitalAddressActivationDto = new IoCourtesyDigitalAddressActivationDto();
        ioCourtesyDigitalAddressActivationDto.setActivationStatus(false);

        Mockito.when(svc.deleteCourtesyAddressBook(Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(Mono.just(new Object()));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ioCourtesyDigitalAddressActivationDto)
                .header(HEADER_API_KEY, "secret")
                .header(HEADER_CX_ID, "abcd")
                .exchange()
                .expectStatus().isNoContent();
    }


    @Test
    void setCourtesyAddressIo_FAIL() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";

        // When

        IoCourtesyDigitalAddressActivationDto ioCourtesyDigitalAddressActivationDto = new IoCourtesyDigitalAddressActivationDto();
        ioCourtesyDigitalAddressActivationDto.setActivationStatus(true);

        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(Mono.error(new PnInternalException("test", "test")));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ioCourtesyDigitalAddressActivationDto)
                .header(HEADER_API_KEY, "secret")
                .header(HEADER_CX_ID, "abcd")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void setCourtesyAddressIoDisableFail() {
        // Given
        String url = "/address-book-io/v1/digital-address/courtesy";

        // When

        IoCourtesyDigitalAddressActivationDto ioCourtesyDigitalAddressActivationDto = new IoCourtesyDigitalAddressActivationDto();
        ioCourtesyDigitalAddressActivationDto.setActivationStatus(false);

        Mockito.when(svc.deleteCourtesyAddressBook(Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(Mono.error(new PnInternalException("test", "test")));

        // Then
        webTestClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ioCourtesyDigitalAddressActivationDto)
                .header(HEADER_API_KEY, "secret")
                .header(HEADER_CX_ID, "abcd")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}