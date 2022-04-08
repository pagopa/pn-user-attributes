package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ConsentDaoTest {
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private DynamoDbAsyncTable<ConsentEntity> userAttributesTable;

    //@Test
    /**
     * Test commentato perchè da revisionare
     */
    void getConsentByType() {

        // GIVEN
        String recipientId = RECIPIENTID;
        ConsentTypeDto consentType = ConsentTypeDto.TOS;

        ConsentEntity userAttributes = new ConsentEntity();
        userAttributes.setRecipientId(recipientId);
        userAttributes.setConsentType(ConsentTypeDto.TOS.getValue());

        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(userAttributes.getRecipientId(), userAttributes.getConsentType()))
                .build();

        UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                .item(userAttributes)
                .ignoreNulls(true)
                .build();


        // WHEN
        Mockito.when(userAttributesTable.getItem(getReq)).thenReturn(CompletableFuture.completedFuture(userAttributes));
        //Mockito.when(userAttributesTable.updateItem(updRequest).thenApply(updRequest);

        // THEN
        userAttributesTable.getItem(getReq);

    }

    protected Key getKeyBuild(String pk, String sk) {
        if (sk == null)
            return Key.builder().partitionValue(pk).build();
        else
            return Key.builder().partitionValue(pk).sortValue(sk).build();
    }
}