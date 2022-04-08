package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Repository
@Slf4j
public class VerificationCodeDao extends BaseDao {
    private final static String DYNAMODB_TABLE_NAME = "${pn.user-attributes.dynamodb.table-name}";
    // Se non è definito un valore di sortKey != non è si riesce a salvare un item in database
    // DA verificare se e' possibile evitare di inserire un valore costante. Stringa vuota non funziona.

    DynamoDbAsyncTable<VerificationCodeEntity> userAttributesTable;

    public VerificationCodeDao(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient,
                               @Value(DYNAMODB_TABLE_NAME) String table) {
        this.userAttributesTable = dynamoDbAsyncClient.table(table, TableSchema.fromBean(VerificationCodeEntity.class));
    }

    // Crea o modifica l'entity VerificationCodeEntity

    /**
     * Inserice o aggiorna un item di tipo VerificationCodeEntity
     * setta i campi verificationCode, created, lastModified
     *
     *
     * @param  userAttributes
     * @return none
     */
    public Mono<Object> saveVerificationCode(VerificationCodeEntity userAttributes) {
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(userAttributes.getPk(), VerificationCodeEntity.SK_VALUE))
                .build();

        return  Mono.fromFuture(userAttributesTable.getItem(getReq).thenApply(r -> {
                    if (r != null) {
                        // update -> don't modify created
                        userAttributes.setCreated(null);
                        if (r.getValidationCode() == userAttributes.getValidationCode())
                            // se il validationCode non cambia non modifico lastModified
                            userAttributes.setLastModified(null);
                    }
                    else
                        // create -> don't set lastModified
                        userAttributes.setLastModified(null);

                    UpdateItemEnhancedRequest<VerificationCodeEntity> updRequest = UpdateItemEnhancedRequest.builder(VerificationCodeEntity.class)
                            .item(userAttributes)
                            .ignoreNulls(true)
                            .build();
                    return userAttributesTable.updateItem(updRequest);
                }));
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
