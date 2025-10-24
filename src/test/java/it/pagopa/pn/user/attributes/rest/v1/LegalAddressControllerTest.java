package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnExceptionInsertingAddress;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.user.attributes.services.utils.ConstantsError.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@WebFluxTest(controllers = {LegalAddressController.class, CourtesyAddressController.class})
class LegalAddressControllerTest {

    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String PN_CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    private static final String PN_CX_TYPE_PF = "PF";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";

    @MockBean
    AddressBookService svc;

    @MockBean
    ConsentsService consentsService;

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    PnUserattributesConfig pnUserattributesConfig;


    @ParameterizedTest(name = "Test deleteRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void deleteRecipientLegalAddress(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        // When
        Mono<Object> voidReturn  = Mono.just("");
        when(svc.deleteLegalAddressBook(anyString(), anyString(), any(), any(), any(), any()))
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

    @ParameterizedTest(name = "Test deleteRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void deleteRecipientLegalAddress_FAIL(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        // When
        when(svc.deleteLegalAddressBook(anyString(), anyString(), any(), any(), any(), any()))
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

    @ParameterizedTest(name = "Test getLegalAddressByRecipient with channelType = {0}")
    @MethodSource("provideChannelTypesDto")
    void getLegalAddressByRecipient(LegalChannelTypeDto channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal";

        LegalAndUnverifiedDigitalAddressDto dto = new LegalAndUnverifiedDigitalAddressDto();
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        dto.setChannelType(channelType);
        Flux<LegalAndUnverifiedDigitalAddressDto> retValue = Flux.just(dto);

        // When
        when(svc.getLegalAddressByRecipient(anyString(), any(), any(),any()))
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

    @ParameterizedTest(name = "Test getLegalAddressBySender with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void getLegalAddressBySender(String channelType) {
        // Given
        String url = "/address-book-private/v1/digital-address/legal/{recipientId}/{senderId}"
                .replace("{recipientId}", RECIPIENTID)
                .replace("{senderId}", channelType);

        LegalDigitalAddressDto dto = new LegalDigitalAddressDto();
        dto.setRecipientId(RECIPIENTID);
        dto.setSenderId(SENDERID);
        Flux<LegalDigitalAddressDto> retValue = Flux.just(dto);

        // When
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString()))
                .thenReturn(retValue);

        // Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest(name = "Test postRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddress(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("12345");
        addressVerification.setValue("+393333300666");


        ConsentDto consentDto = ConsentDto.builder().consentType(ConsentTypeDto.TOS_SERCQ).recipientId("recipientId")
                .accepted(true).build();
        ConsentDto consentDto1 = ConsentDto.builder().consentType(ConsentTypeDto.DATAPRIVACY_SERCQ). recipientId("recipientId")
                .accepted(true).build();

        CourtesyDigitalAddressDto courtesyEmail = new CourtesyDigitalAddressDto();
        courtesyEmail.setChannelType(CourtesyChannelTypeDto.EMAIL);
        courtesyEmail.setValue("test@example.com");

        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.TOS_SERCQ), any()))
                .thenReturn(Mono.just(consentDto));
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.DATAPRIVACY_SERCQ), any()))
                .thenReturn(Mono.just(consentDto1));
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.empty());
        when(svc.getCourtesyAddressByRecipient(any(), any(),any(),any()))
                .thenReturn(Flux.just(courtesyEmail));
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @ParameterizedTest(name = "Test postRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddressActivationSERCQError(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("12345");
        addressVerification.setValue("+393333300666");


        ConsentDto consentDto = ConsentDto.builder().consentType(ConsentTypeDto.TOS_SERCQ).recipientId("recipientId")
                .accepted(true).build();
        ConsentDto consentDto1 = ConsentDto.builder().consentType(ConsentTypeDto.DATAPRIVACY_SERCQ). recipientId("recipientId")
                .accepted(true).build();
        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.TOS_SERCQ), any()))
                .thenReturn(Mono.just(consentDto));
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.DATAPRIVACY_SERCQ), any()))
                .thenReturn(Mono.just(consentDto1));
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.empty());
        when(svc.getCourtesyAddressByRecipient(any(), any(),any(),any()))
                .thenReturn(Flux.empty());
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);


        WebTestClient.RequestHeadersSpec<?> request = webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF);

        if ("SERCQ".equals(channelType)) {
            request.exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("ERROR_DURING_SERCQ_ACTIVATING")
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.errors[0].code").isEqualTo("PN_HTTPRESPONSE_GENERIC_ERROR")
                    .jsonPath("$.errors[0].detail").exists();
        } else {
            request.exchange()
                    .expectStatus().isNoContent();
        }
    }

    @ParameterizedTest(name = "Test postRecipientLegalAddress SERCQ KO with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddress_SercqKo(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        // Forzo direttamente l'eccezione quando il servizio viene chiamato
        ConsentDto consentDto = ConsentDto.builder().consentType(ConsentTypeDto.TOS_SERCQ).recipientId("recipientId")
                .accepted(true).build();
        ConsentDto consentDto1 = ConsentDto.builder().consentType(ConsentTypeDto.DATAPRIVACY_SERCQ). recipientId("recipientId")
                .accepted(true).build();

        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.TOS_SERCQ), any()))
                .thenReturn(Mono.just(consentDto));
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.DATAPRIVACY_SERCQ), any()))
                .thenReturn(Mono.just(consentDto1));
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.empty());
        when(svc.getCourtesyAddressByRecipient(any(), any(), any(), any()))
                .thenThrow(new PnExceptionInsertingAddress(
                        ERROR_ACTIVATION_LEGAL_SERCQ_DETAIL));
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);

        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);

        // Corpo della request
        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setValue("+393333300666");
        addressVerification.setVerificationCode("12345");

        // When & Then
        WebTestClient.ResponseSpec response = webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange();

        response.expectBody(String.class).consumeWith(result -> {
            String body = result.getResponseBody();
            log.info("=== Response Body ===");
            log.info(body);
            log.info("=====================");
        });

        if ("SERCQ".equals(channelType)) {
            response.expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo(ERROR_TITLE_LEGAL)
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.detail").isEqualTo(ERROR_ACTIVATION_LEGAL)
                    .jsonPath("$.errors[0].code").isEqualTo("PN_HTTPRESPONSE_GENERIC_ERROR")
                    .jsonPath("$.errors[0].detail").isEqualTo(ERROR_ACTIVATION_LEGAL_SERCQ_DETAIL);
        } else {
            response.expectStatus().isNoContent();
        }
    }


    @ParameterizedTest(name = "Test postRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddress_FAIL(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("12345");
        addressVerification.setValue("00393333300666");

        // When
        when(consentsService.getConsents(anyString(), any(CxTypeAuthFleetDto.class)))
                .thenReturn(Flux.fromIterable(List.of(ConsentDto.builder()
                        .recipientId("recipientId")
                        .consentType(ConsentTypeDto.TOS_SERCQ)
                        .build())));
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException()));
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    @ParameterizedTest(name = "Test postRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddressVerCodeNeeded(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setValue("test@email.com");
        CourtesyDigitalAddressDto courtesyEmail = new CourtesyDigitalAddressDto();
        courtesyEmail.setChannelType(CourtesyChannelTypeDto.EMAIL);

        ConsentDto consentDto = ConsentDto.builder().consentType(ConsentTypeDto.TOS_SERCQ).recipientId("recipientId")
                .accepted(true).build();
        ConsentDto consentDto1 = ConsentDto.builder().consentType(ConsentTypeDto.DATAPRIVACY_SERCQ). recipientId("recipientId")
                .accepted(true).build();
        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED);
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.TOS_SERCQ), any()))
                .thenReturn(Mono.just(consentDto));
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.DATAPRIVACY_SERCQ), any()))
                .thenReturn(Mono.just(consentDto1));
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.empty());
        when(svc.getCourtesyAddressByRecipient(any(),any(), any(),any())).thenReturn(Flux.just(courtesyEmail));
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(true);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @ParameterizedTest(name = "Test postRecipientLegalAddress with channelType = {0}")
    @MethodSource("provideChannelTypes")
    void postRecipientLegalAddressVerCodeFail(String channelType) {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("verification");
        addressVerification.setValue("value");

        // When
        when(consentsService.getConsents(anyString(), any(CxTypeAuthFleetDto.class)))
                .thenReturn(Flux.fromIterable(List.of(ConsentDto.builder()
                        .recipientId("recipientId")
                        .consentType(ConsentTypeDto.TOS_SERCQ)
                        .build())));
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), anyList(), any(), any()))
                .thenThrow(new PnInvalidVerificationCodeException());

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    private static Stream<Arguments> provideChannelTypes(){
        return Stream.of(Arguments.of("PEC"), Arguments.of("APPIO"), Arguments.of("SERCQ"));

    }

    private static Stream<Arguments> provideChannelTypesDto() {
        return Stream.of(Arguments.of(LegalChannelTypeDto.PEC), Arguments.of(LegalChannelTypeDto.APPIO), Arguments.of(LegalChannelTypeDto.SERCQ));
    }

    @Test
    void SercqDisabled() {
        String channelType= LegalChannelTypeDto.SERCQ.getValue();

        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", channelType);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setVerificationCode("12345");
        addressVerification.setValue("+393333300666");


        ConsentDto consentDto = ConsentDto.builder().consentType(ConsentTypeDto.TOS_SERCQ).recipientId("recipientId")
                .accepted(true).build();
        ConsentDto consentDto1 = ConsentDto.builder().consentType(ConsentTypeDto.DATAPRIVACY_SERCQ). recipientId("recipientId")
                .accepted(true).build();
        // When
        Mono<AddressBookService.SAVE_ADDRESS_RESULT> voidReturn  = Mono.just(AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS);
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.TOS_SERCQ), any()))
                .thenReturn(Mono.just(consentDto));
        when(consentsService.getConsentByType(anyString(), eq(CxTypeAuthFleetDto.PF), eq(ConsentTypeDto.DATAPRIVACY_SERCQ), any()))
                .thenReturn(Mono.just(consentDto1));
        when(svc.getLegalAddressByRecipientAndSender(anyString(), anyString())).thenReturn(Flux.empty());
        when(svc.saveLegalAddressBook(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(voidReturn);
        when(pnUserattributesConfig.isSercqEnabled()).thenReturn(false);

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isBadRequest();

    }


}