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
import utils.DateFormatUtils;

import java.time.Instant;

@Repository
@Slf4j
public class ConsentDao extends BaseDao {

    private static final String CONSENT_PK_PREFIX = "CO#";

    DynamoDbAsyncTable<ConsentEntity> userAttributesTable;

    public ConsentDao(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient,
                      @Value("${pn.user-attributes.dynamodb.table-name}") String table) {
        this.userAttributesTable = dynamoDbAsyncClient.table(table, TableSchema.fromBean(ConsentEntity.class));
    }


    public Mono<Object> consentAction(ConsentEntity userAttributes){
        userAttributes.setPk(CONSENT_PK_PREFIX + userAttributes.getPk());
        userAttributes.setInsertDate(DateFormatUtils.formatInstantToIso8601String(Instant.now()));
/*
        AttributeValue rightnow = AttributeValue.builder()
                .s()
                .build();

        Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":right_now", rightnow);
        Expression exp = Expression
                .builder()
                .expression("SET if_not_exists(insertDate, :right_now)")
                .expressionValues(expressionValues)
                .build();
*/

        log.debug("key pk {} - sk {}", userAttributes.getPk(), userAttributes.getSk());

        UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                .item(userAttributes)
                .ignoreNulls(true)
                .build();
        return Mono.fromFuture(userAttributesTable.updateItem(updRequest));
/*
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(userAttributes.getPk(), userAttributes.getSk()))
                .build();

        return Mono.fromFuture(() -> userAttributesTable.getItem(getReq))
                .zipWhen(r -> {
                    log.info("fromFuture");
                    if (r != null)
                        userAttributes.setInsertDate(null);

                    UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                            .item(userAttributes)
                            .ignoreNulls(true)
                            .build();
                    return Mono.fromFuture(userAttributesTable.updateItem(updRequest));
                }, (r,u) -> Mono.empty());
*/
    }

    public Mono<ConsentEntity> getConsentByType(String recipientId, ConsentTypeDto consentType) {

        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(recipientId, consentType.getValue()))
                .build();

        return Mono.fromFuture(userAttributesTable.getItem(getReq));

    }

    public Flux<ConsentEntity> getConsents(String recipientId) {

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(CONSENT_PK_PREFIX + recipientId)))
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
