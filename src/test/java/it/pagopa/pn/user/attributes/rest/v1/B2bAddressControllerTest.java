package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {B2bAddressController.class})
public class B2bAddressControllerTest {
    private static final String SENDERID = "default";
    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";
    private static final String PN_CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    private static final String PN_CX_TYPE_PF = "PF";

    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void getLegalAddressBySenderTest() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}"
                .replace("{senderId}", SENDERID);

        LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
        dto.setSenderId(SENDERID);
        Flux<LegalDigitalAddressDto> retValue = Flux.just(dto);

        // When
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}
