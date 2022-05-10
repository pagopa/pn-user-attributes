package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.exceptions.InvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
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

@WebFluxTest(controllers = {LegalAddressController.class})
class LegalAddressControllerTest {
    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";
    private static final String CHANNELTYPE = "PEC";

    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void deleteRecipientLegalAddress() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        // When
        Mono<Object> voidReturn  = Mono.just("");
        Mockito.when(svc.deleteLegalAddressBook(Mockito.anyString(),
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
    void getLegalAddressByRecipient() {
        // Given
        String url = "/address-book/v1/digital-address/legal";

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
                .header(PA_ID, RECIPIENTID)
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
        Mockito.when(svc.getLegalAddressByRecipientAndSender(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postRecipientLegalAddress() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        Mockito.when(svc.saveLegalAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isNoContent();
    }


    @Test
    void postRecipientLegalAddressVerCodeNeeded() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        Mockito.when(svc.saveLegalAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postRecipientLegalAddressVerCodeFail() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        Mockito.when(svc.saveLegalAddressBook(Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any()))
                .thenThrow(new InvalidVerificationCodeException());

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().is4xxClientError();
    }

}