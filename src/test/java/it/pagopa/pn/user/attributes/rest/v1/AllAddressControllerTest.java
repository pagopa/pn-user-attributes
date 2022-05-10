package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.UserAddressesDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(controllers = {AllAddressController.class})
class AllAddressControllerTest {
    private static final String PA_ID = "x-pagopa-pn-cx-id";
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
        List<LegalDigitalAddressDto> l_list = new ArrayList<>();
        userAddressesDto.setCourtesy(c_list);
        userAddressesDto.setLegal(l_list);

        // When
        Mono<UserAddressesDto> voidReturn  = Mono.just(userAddressesDto);

        // Then
        Mockito.when(svc.getAddressesByRecipient(Mockito.anyString()))
                .thenReturn(voidReturn);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isOk()
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
        Mockito.when(svc.getAddressesByRecipient(Mockito.anyString()))
                .thenReturn(voidReturn);

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isOk().expectBody(UserAddressesDto.class);
    }
}