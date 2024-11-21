package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDataVaultClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto.LEGAL;
import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VerificationCodeUtilsTest {

    @Mock
    private VerificationCodeUtils verificationCodeUtils;
    @Mock
    private AddressBookApi addressBookApi;
    @Mock
    private AddressBookDao addressBookDao;
    @Mock
    private PnUserattributesConfig pnUserattributesConfig;
    @Mock
    private PnExternalChannelClient externalChannelClient;
    @Mock
    private VerifiedAddressUtils verifiedAddressUtils;
    @Mock
    private PnDataVaultClient dataVaultClient;
    private static final String RECIPIENT_ID = "PF-12345-678910-12345-678910";

    @BeforeEach
    void beforeEach() {
        verificationCodeUtils = new VerificationCodeUtils(addressBookDao, pnUserattributesConfig, dataVaultClient, externalChannelClient, verifiedAddressUtils);
    }

    @Test
    void validateVerificationCodeAndSendToDataVault_ValuedAddress() {
        //GIVEN
        String realAddress = "pec@example.com";
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(RECIPIENT_ID,
                hashAddress(realAddress),
                LegalChannelTypeDto.PEC.getValue(),
                "default",
                LEGAL.getValue(),
                null);

        //WHEN
        when(dataVaultClient.updateRecipientAddressByInternalId(eq(RECIPIENT_ID), any(), eq(realAddress))).thenReturn(Mono.empty());
        when(verifiedAddressUtils.saveInDynamodb(any(), any())).thenReturn(Mono.empty());

        //THEN
        verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCodeEntity, List.of(), "pec@example.com").block();

        verify(dataVaultClient, never()).getVerificationCodeAddressByInternalId(any(), any());
    }

    @Test
    void validateVerificationCodeAndSendToDataVault_NullAddress() {
        //GIVEN
        String realAddress = "pec@example.com";
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(RECIPIENT_ID,
                hashAddress(realAddress),
                LegalChannelTypeDto.PEC.getValue(),
                "default",
                LEGAL.getValue(),
                null);

        //WHEN
        when(verifiedAddressUtils.saveInDynamodb(any(AddressBookEntity.class), anyList())).thenReturn(Mono.empty());

        //THEN
        StepVerifier.create(verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCodeEntity, List.of(), null)).expectError(PnInternalException.class);
    }

    @Test
    void validateVerificationCodeAndSendToDataVault_AddressNotFound() {
        //GIVEN
        String realAddress = "nonexisting@example.com";
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(RECIPIENT_ID,
                hashAddress(realAddress),
                LegalChannelTypeDto.PEC.getValue(),
                "default",
                LEGAL.getValue(),
                null);

        //WHEN
        when(verifiedAddressUtils.saveInDynamodb(any(AddressBookEntity.class), anyList())).thenReturn(Mono.empty());

        //THEN
        StepVerifier.create(verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCodeEntity, List.of(), realAddress)).expectError(PnInternalException.class);
    }

}
