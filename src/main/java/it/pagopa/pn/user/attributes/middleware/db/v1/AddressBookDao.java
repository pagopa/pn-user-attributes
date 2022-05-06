package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressDeletionFailure;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressNotFound;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressesNotFound;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.BaseEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerifiedAddressEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.user.attributes.exceptions.RestWebExceptionHandler.findExceptionRootCause;

@Repository
@Slf4j
public class AddressBookDao extends BaseDao {

    DynamoDbAsyncTable<AddressBookEntity> addressBookTable;
    DynamoDbAsyncTable<VerificationCodeEntity> verificationCodeTable;
    DynamoDbAsyncTable<VerifiedAddressEntity> verifiedAddressTable;
    DynamoDbAsyncClient dynamoDbAsyncClient;
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    String table;

    public AddressBookDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                          DynamoDbAsyncClient dynamoDbAsyncClient,
                          PnUserattributesConfig pnUserattributesConfig
                          ) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.table = pnUserattributesConfig.getDynamodbTableName();
        this.addressBookTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(AddressBookEntity.class));
        this.verificationCodeTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(VerificationCodeEntity.class));
        this.verifiedAddressTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(VerifiedAddressEntity.class));
    }

    // Crea o modifica l'entity VerificationCodeEntity

    public Mono<Object> deleteAddressBook(String recipientId, String senderId, String legal, String channelType) {
        AddressBookEntity addressBook = new AddressBookEntity(recipientId, legal, senderId, channelType);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk",  AttributeValue.builder().s(addressBook.getPk()).build());

        Expression exp = Expression.builder()
                .expression(BaseEntity.COL_PK + " = :pk")
                .expressionValues(expressionValues)
                .build();

        DeleteItemEnhancedRequest delRequest = DeleteItemEnhancedRequest.builder()
                .key(getKeyBuild(addressBook.getPk(), addressBook.getSk()))
                .conditionExpression(exp)
                .build();


        return Mono.fromFuture(addressBookTable.deleteItem(delRequest)
                .exceptionally(throwable -> {
                    Throwable rootCause = findExceptionRootCause(throwable);
                    if (rootCause instanceof ConditionalCheckFailedException)
                        throw new PnDigitalAddressNotFound();
                    else {
                        throw new PnDigitalAddressDeletionFailure();
                    }
                }));

    }

    public Flux<AddressBookEntity> getAddresses(String recipientId, String senderId, String legalType) {
        AddressBookEntity addressBook = new AddressBookEntity(recipientId, legalType, senderId, null);


        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(addressBook.getPk(), addressBook.getSk())))
                .scanIndexForward(true)
                .build();

        return Flux.from(addressBookTable.query(qeRequest)
                        .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));
    }


    public Flux<AddressBookEntity> getAllAddressesByRecipient(String recipientId) {
        AddressBookEntity addressBook = new AddressBookEntity(recipientId, null, null, null);

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(addressBook.getPk())))
                .scanIndexForward(true)
                .build();


        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));

    }

    public Mono<VerificationCodeEntity> saveVerificationCode(VerificationCodeEntity entity)
    {
        return Mono.fromFuture(verificationCodeTable.updateItem(entity));
    }


    public Mono<VerificationCodeEntity> getVerificationCode(VerificationCodeEntity entity)
    {
        return Mono.fromFuture(verificationCodeTable.getItem(entity));
    }

    public Mono<VerifiedAddressEntity> getVerifiedAddress(String recipientId, String hashedAddress)
    {
        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(recipientId, hashedAddress, "");

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(verifiedAddressEntity.getPk(), verifiedAddressEntity.getSk())))
                .scanIndexForward(true)
                .build();


        return Flux.from(verifiedAddressTable.query(qeRequest)
                        .items()).collectList()
                .map(list -> list.isEmpty()?null:list.get(0));
    }

    /**
     * Inserice o aggiorna un item di tipo AddressBookEntity e VerifiedAddress
     *
     * @param addressBook indirizzo da salvare
     * @param verifiedAddress verifiedaddress da salvare
     *
     * @return void
     */
    public Mono<Void> saveAddressBookAndVerifiedAddress(AddressBookEntity addressBook, VerifiedAddressEntity verifiedAddress) {

        TransactUpdateItemEnhancedRequest<AddressBookEntity> updRequest = TransactUpdateItemEnhancedRequest.builder(AddressBookEntity.class)
                .item(addressBook)
                .build();
        TransactUpdateItemEnhancedRequest<VerifiedAddressEntity> updVARequest = TransactUpdateItemEnhancedRequest.builder(VerifiedAddressEntity.class)
                .item(verifiedAddress)
                .build();

        TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                .addUpdateItem(addressBookTable, updRequest)
                .addUpdateItem(verifiedAddressTable, updVARequest)
                .build();

        return Mono.fromFuture(dynamoDbEnhancedAsyncClient.transactWriteItems(transactWriteItemsEnhancedRequest));
    }

}
