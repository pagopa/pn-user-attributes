package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.v1.AddressBookService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {LegalAddressController.class})
class LegalAddressControllerTest {
    private static final String PA_ID = "PA_ID";
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CHANNELTYPE = "PEC";

    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void deleteRecipientLegalAddress() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/legal/{senderId}/{channelType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        // When
        Mono<Object> voidReturn  = Mono.just("");
        Mockito.when(svc.deleteAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.eq(true),
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
    void getLegalAddressByRecipient() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/legal"
                .replace("{recipientId}", RECIPIENTID);

        LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        dto.setChannelType(LegalChannelTypeDto.APPIO);
        Flux<LegalDigitalAddressDto> retValue = Flux.just(dto);

        // When
        Mockito.when(svc.getLegalAddressByRecipient(Mockito.anyString()))
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
    void getLegalAddressBySender() {
        // Given
        String url = "/address-book-private/v1/digital-address/legal/{recipientId}/{senderId}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID);

        LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        Flux<LegalDigitalAddressDto> retValue = Flux.just(dto);

        // When
        Mockito.when(svc.getLegalAddressBySender(Mockito.anyString(), Mockito.anyString()))
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
    void postRecipientLegalAddress() {
        // Given
        String url = "/address-book/v1/digital-address/{recipientId}/legal/{senderId}/{channelType}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        // When
        Mono<Boolean> voidReturn  = Mono.just(true);
        Mockito.when(svc.saveAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.eq(true),
                        Mockito.anyString(),
                        Mockito.any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(addressVerification), AddressVerificationDto.class)
                .header(PA_ID)
                .exchange()
                .expectStatus().isOk();
    }

}