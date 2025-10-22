package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.LocalStackTestConfig;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.TestDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.AddressVerificationResponseDto.ResultEnum.CODE_VERIFICATION_REQUIRED;
import static it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.AddressVerificationResponseDto.ResultEnum.PEC_VALIDATION_REQUIRED;
import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(LocalStackTestConfig.class)
@SpringBootTest
@AutoConfigureWebTestClient
class LegalAddressControllerWrapperTestIT {

    private static final String PA_ID = "x-pagopa-pn-cx-id";
    private static final String PN_CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    private static final String PN_CX_TYPE_PF = "PF";
    private static final String RECIPIENTID = "PF-123e4567-e89b-12d3-a456-426614174000";
    private static final String SENDERID = "default";

    @MockBean
    PnDataVaultClient dataVaultClient;
    @MockBean
    PnExternalRegistryClient externalRegistryClient;
    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Autowired
    PnUserattributesConfig pnUserattributesConfig;
    @Autowired
    WebTestClient webTestClient;
    @Autowired
    AddressBookDao addressBookDao;
    @MockBean
    LegalAddressController legalAddress;
    TestDao<VerificationCodeEntity> testDao;

    @BeforeEach
    void setup() {
        testDao = new TestDao<>(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), VerificationCodeEntity.class);
    }
    @Test
    void postRecipientLegalAddress_PEC_CodeVerificationRequired() {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", LegalChannelTypeDto.PEC.getValue());

        String realAddress = "test@pec.it";

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setValue(realAddress);
        AddressVerificationResponseDto expectedResponse = new AddressVerificationResponseDto(); expectedResponse.setResult(CODE_VERIFICATION_REQUIRED);
        // When
        Map<String, AddressDtoDto> addresses = Map.of(LegalAddressTypeDto.LEGAL + "#" + "default" + "#" + "PEC", new AddressDtoDto().value(realAddress));
        when(dataVaultClient.getRecipientAddressesByInternalId(RECIPIENTID)).thenReturn(Mono.just(new RecipientAddressesDtoDto().addresses(addresses)));
        when(dataVaultClient.updateRecipientAddressByInternalId(anyString(), anyString(), anyString(), any(BigDecimal.class))).thenReturn(Mono.empty());
        when(externalRegistryClient.getAooUoIdsApi(List.of(SENDERID))).thenReturn(Flux.empty());
        when(legalAddress.postRecipientLegalAddress( anyString(), any(), anyString(), any(), any(), any(), any(), any()) ).thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));
        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AddressVerificationResponseDto.class)
                .value(Matchers.hasProperty("result", Matchers.equalTo(CODE_VERIFICATION_REQUIRED)));
    }

    @Test
    void postRecipientLegalAddress_PEC_ValidationRequired() throws ExecutionException, InterruptedException {
        // Given
        String url = "/address-book/v1/digital-address/legal/{senderId}/{channelType}"
                .replace("{senderId}", SENDERID)
                .replace("{channelType}", LegalChannelTypeDto.PEC.getValue());

        String realAddress = "test@pec.it";
        String hashedAddress = hashAddress(realAddress);

        AddressVerificationDto addressVerification = new AddressVerificationDto();
        addressVerification.setValue(realAddress);
        addressVerification.setVerificationCode("12345");

        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(RECIPIENTID, hashedAddress, LegalChannelTypeDto.PEC.getValue(), SENDERID, "LEGAL", null);
        verificationCodeEntity.setVerificationCode("12345");
        testDao.put(verificationCodeEntity);

        AddressVerificationResponseDto expectedResponse = new AddressVerificationResponseDto(); expectedResponse.setResult(PEC_VALIDATION_REQUIRED);

        // When
        Map<String, AddressDtoDto> addresses = Map.of(LegalAddressTypeDto.LEGAL + "#" + "default" + "#" + "PEC", new AddressDtoDto().value(realAddress));
        when(dataVaultClient.getRecipientAddressesByInternalId(RECIPIENTID)).thenReturn(Mono.just(new RecipientAddressesDtoDto().addresses(addresses)));
        when(externalRegistryClient.getAooUoIdsApi(List.of(SENDERID))).thenReturn(Flux.empty());
        when(legalAddress.postRecipientLegalAddress( anyString(), any(), anyString(), any(), any(), any(), any(), any()) ).thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));

        // Then
        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(addressVerification)
                .header(PA_ID, RECIPIENTID)
                .header(PN_CX_TYPE_HEADER, PN_CX_TYPE_PF)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AddressVerificationResponseDto.class)
                .value(Matchers.hasProperty("result", Matchers.equalTo(PEC_VALIDATION_REQUIRED)));
    }

}