package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {CourtesyAddressController.class})
class CourtesyAddressControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CHANNELTYPE = "SMS";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    AddressBookService svc;


    @Test
    void postRecipientCourtesyAddress() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy/{senderId}/{channelType}"
                    .replace("{recipientId}", RECIPIENTID)
                    .replace("{senderId}", SENDERID)
                    .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("verification");
        addressVerificationDto.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveAddressBook(Mockito.anyString(),
                                         Mockito.anyString(),
                                         Mockito.eq(false),
                                         Mockito.anyString(),
                                         Mockito.any()))
                .thenReturn(voidReturn);


        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(addressVerificationDto), AddressVerificationDto.class)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteRecipientCourtesyAddress() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy/{senderId}/{channelType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);


        // When
        Mono<Object> voidReturn  = Mono.just("");
        Mockito.when(svc.deleteAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.eq(false),
                        Mockito.anyString()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.delete()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void getCourtesyAddressByRecipient() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/courtesy"
                .replace("{recipientId}", RECIPIENTID);
        CourtesyDigitalAddressDto dto = new CourtesyDigitalAddressDto();
        Flux<CourtesyDigitalAddressDto> retValue = Flux.just(dto);
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        dto.setChannelType(CourtesyChannelTypeDto.APPIO);


        // When
        Mockito.when(svc.getCourtesyAddressByRecipient(Mockito.anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCourtesyAddressBySender() {
        // Given
        String url = "/address-book-private/v1/digital-address/courtesy/{recipientId}/{senderId}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID);

        // When
        CourtesyDigitalAddressDto dto = new CourtesyDigitalAddressDto();
        Flux<CourtesyDigitalAddressDto> retValue = Flux.just(dto);
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        dto.setChannelType(CourtesyChannelTypeDto.APPIO);

        Mockito.when(svc.getCourtesyAddressBySender(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }
}