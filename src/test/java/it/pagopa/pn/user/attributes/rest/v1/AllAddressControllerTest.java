package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAndUnverifiedDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.UserAddressesDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {AllAddressController.class})
class AllAddressControllerTest {

    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String PN_CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    private static final String PN_CX_TYPE_PF = "PF";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    AddressBookService svc;

    @Test
    void getAddressesByRecipient() {
        // Given
        String url = "/address-book/v1/digital-address";

        UserAddressesDto userAddressesDto = new UserAddressesDto();
        List<CourtesyDigitalAddressDto> c_list = new ArrayList<>();
        List<LegalAndUnverifiedDigitalAddressDto> l_list = new ArrayList<>();
        userAddressesDto.setCourtesy(c_list);
        userAddressesDto.setLegal(l_list);

        // When
        Mono<UserAddressesDto> voidReturn  = Mono.just(userAddressesDto);

        // Then
        when(svc.getAddressesByRecipient(anyString(), any(), any(), any()))
                .thenReturn(voidReturn);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserAddressesDto.class);
    }

    @Test
    void getAddressesByRecipient_No_Addresses() {
        // Given
        String url = "/address-book/v1/digital-address";

        UserAddressesDto userAddressesDto = new UserAddressesDto();
        userAddressesDto.setLegal(null);
        userAddressesDto.setCourtesy(null);

        // When
        Mono<UserAddressesDto> voidReturn  = Mono.just(userAddressesDto);

        // Then
        when(svc.getAddressesByRecipient(anyString(), any(), any(), any()))
                .thenReturn(voidReturn);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserAddressesDto.class);
    }
}