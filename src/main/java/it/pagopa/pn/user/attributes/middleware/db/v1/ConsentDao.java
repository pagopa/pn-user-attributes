package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Repository
@Slf4j
public class ConsentDao extends BaseDao {
    DynamoDbAsyncTable<ConsentEntity> userAttributesTable;

    public ConsentDao(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient,
                      @Value("${pn.user-attributes.dynamodb.table-name}") String table) {
        this.userAttributesTable = dynamoDbAsyncClient.table(table, TableSchema.fromBean(ConsentEntity.class));
    }

    // Crea o modifica l'entity ConsentEntity

    /**
     * Inserice o aggiorna un item di tipo ConsentEntity
     * setta i campi accepted, created, lastModified
     * @param userAttributes
     * @return none
     */
    public Mono<Object> consentAction(ConsentEntity userAttributes){
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(userAttributes.getRecipientId(), userAttributes.getConsentType()))
                .build();

        return  Mono.fromFuture(userAttributesTable.getItem(getReq).thenApply(r -> {
                    if (r != null) {
                        // update -> don't modify created
                        userAttributes.setCreated(null);
                        if (r.isAccepted() == userAttributes.isAccepted())
                            // se il consenso non cambia non modifico lastModified
                            userAttributes.setLastModified(null);
                    }
                    else
                        // create -> don't set lastModified
                        userAttributes.setLastModified(null);

                    UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                            .item(userAttributes)
                            .ignoreNulls(true)
                            .build();
                    return userAttributesTable.updateItem(updRequest);
                }));
    }

    /**
     * Legge l'entity ConsentEntity associata a recipientId e ConsentType (TOS/DATAPRIVACY)
     *
     * @param recipientId
     * @param consentType
     * @return ConsentEntity
     */
     public Mono<ConsentEntity> getConsentByType(String recipientId, ConsentTypeDto consentType) {

        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(ConsentEntity.getPk(recipientId), consentType.getValue()))
                .build();

        return Mono.fromFuture(userAttributesTable.getItem(getReq));
    }

    /*
        Legge la lista (al massimo due elementi) di entity ConsentEntity associata a recipientId
     */
    public Flux<ConsentEntity> getConsents(String recipientId) {

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(ConsentEntity.getPk(recipientId))))
                .scanIndexForward(true)
                .build();

        return Flux.from(userAttributesTable.query(qeRequest))
                .flatMapIterable(mlist -> {
                    return mlist.items();
                });

    }

    protected Key getKeyBuild(String pk) {
        return getKeyBuild(pk, null);
    }

    protected Key getKeyBuild(String pk, String sk) {
        if (sk == null)
            return Key.builder().partitionValue(pk).build();
        else
            return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

}
