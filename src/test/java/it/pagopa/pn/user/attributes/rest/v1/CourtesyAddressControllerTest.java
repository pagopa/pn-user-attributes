package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = {CourtesyAddressController.class})
class CourtesyAddressControllerTest {

    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String PN_CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    private static final String PN_CX_TYPE_PF = "PF";
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
        addressVerificationDto.setValue("test@email.com");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        when(svc.saveCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void postRecipientCourtesyAddress_FAIL() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("12345");
        addressVerificationDto.setValue("+393333300666");

        // When
        when(svc.saveCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    @Test
    void postRecipientCourtesyAddressVerCodeNeeded() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);

        AddressVerificationDto addressVerificationDto = new AddressVerificationDto();
        addressVerificationDto.setVerificationCode("12345");
        addressVerificationDto.setValue("00393333300666");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        when(svc.saveCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk();
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
        when(svc.saveCourtesyAddressBook(anyString(), anyString(), any(), any()))
                .thenThrow(new PnInvalidVerificationCodeException());

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerificationDto)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void deleteRecipientCourtesyAddress() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);


        // When
        Mono<Object> voidReturn  = Mono.just("");
        when(svc.deleteCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(voidReturn);

        // Then
        webTestClient.delete()
                .uri(url)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void deleteRecipientCourtesyAddress_FAIL() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CHANNELTYPE);


        // When
        when(svc.deleteCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));

        // Then
        webTestClient.delete()
                .uri(url)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    @Test
    void deleteRecipientCourtesyAddress_FAIL_whenEmailHasSercqAddress() {
        // Given
        String url = "/address-book/v1/digital-address/courtesy/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", CourtesyChannelTypeDto.EMAIL.name());

        LegalDigitalAddressDto sercqAddress = new LegalDigitalAddressDto();
        sercqAddress.setChannelType(LegalChannelTypeDto.SERCQ);

        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString()))
                .thenReturn(Flux.just(sercqAddress));

        // Quando / Then
        webTestClient.delete()
                .uri(url)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isBadRequest();

        // Verifica che deleteCourtesyAddressBook NON venga chiamata
        verify(svc, never()).deleteCourtesyAddressBook(anyString(), anyString(), any(), any(), any(), any());
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
        when(svc.getCourtesyAddressByRecipient(anyString(), any(), any(), any()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk();
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

        when(svc.getCourtesyAddressByRecipientAndSender(anyString(), anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}