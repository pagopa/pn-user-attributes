package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
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
    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CHANNELTYPE = "SMS";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    AddressBookService svc;


    @Test
    void postRecipientCourtesyAddress() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                    .replace("{senderId}", SENDERID)
                    .replace("{channelType}", CHANNELTYPE);

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
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void postRecipientCourtesyAddressVerCodeNeeded() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("12345");
        addressVerificationDto.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);


        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postRecipientCourtesyAddressVerCodeFailed() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("12345");
        addressVerificationDto.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        Mockito.when(svc.saveCourtesyAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenThrow(new InvalidVerificationCodeException());


        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void deleteRecipientCourtesyAddress() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);


        // When
        Mono<Object> voidReturn  = Mono.just("");
        Mockito.when(svc.deleteCourtesyAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.delete()
                .uri(url)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void getCourtesyAddressByRecipient() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy";
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
                .header(PA_ID, RECIPIENTID)
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

        Mockito.when(svc.getCourtesyAddressByRecipientAndSender(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}