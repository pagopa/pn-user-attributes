package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressDeletionFailure;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressNotFound;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressesNotFound;
import it.pagopa.pn.user.attributes.exceptions.PnVerificationCodeInvalid;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerifiedAddressEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
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

import java.time.Instant;
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
                .expression(AddressBookEntity.COL_PK + " = :pk")
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
     * Inserice o aggiorna un item di tipo AddressBookEntity
     * setta i campi address, created, lastModified
     *     *
     * @param  addressBook
     * @return none
     */
    public Mono<Object> saveAddressBook(AddressBookEntity addressBook) {
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(addressBook.getPk(), addressBook.getSk()))
                .build();

        return  Mono.fromFuture(addressBookTable.getItem(getReq).thenCompose(r -> {
            if (r != null) {
                // update -> don't modify created
                addressBook.setCreated(null);
                if (r.getAddress().equals(addressBook.getAddress()))
                    // se address non cambia non modifico lastModified
                    addressBook.setLastModified(null);
            }
            else
                // create -> don't set lastModified
                addressBook.setLastModified(null);

            UpdateItemEnhancedRequest<AddressBookEntity> updRequest = UpdateItemEnhancedRequest.builder(AddressBookEntity.class)
                    .item(addressBook)
                    .ignoreNulls(true)
                    .build();
            return addressBookTable.updateItem(updRequest);
        }));
    }
/*
    public Mono<Boolean> saveAddressBookEx(String recipientId,
                                           String senderId,
                                           String legalType,
                                           String channelType,
                                           String address,
                                           final String verificationCode) {


        return getVerifiedAddress(recipientId, channelType, address).zipWhen(r -> {
            if (r.getPk() == null) {
                log.debug("address not verified - recipientId: {} - channelType: {}", recipientId, channelType);

                if (StringUtils.hasText(verificationCode)) {
                    log.debug("verificationCode is not empty");
                    return getVerificationCode(recipientId, channelType, address).zipWhen(xr -> {
                        if (xr.getPk() == null) {
                            log.debug("VerificationCodeEntity not found");
                            throw new PnVerificationCodeInvalid();
                        } else {
                            log.debug("VerificationCodeEntity found");

                            if (xr.getVerificationCode().equals(verificationCode)) {
                                log.debug("verificationCode matched");

                                 return updateAddressBook(false
                                                         ,recipientId
                                                         ,senderId
                                                         ,legalType
                                                         ,channelType
                                                         ,address
                                                         ,verificationCode);

                            } else {
                                log.debug("verificationCode NOT matched");
                                throw new PnVerificationCodeInvalid();
                            }
                        }
                    }, (xr, xr1) -> xr1);

                } else {
                    log.debug("verificationCode is empty");
                    // genera provvisoriamente un codice di verifica e scrive su VerificationCodeTable
                    String newVerificationCode = getNewVerificationCode();

                    VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
                    verificationCodeEntity.setPk(VerificationCodeEntity.getPk(recipientId, channelType, address));
                    verificationCodeEntity.setSk(VerificationCodeEntity.SK_VALUE);
                    verificationCodeEntity.setVerificationCode(newVerificationCode);
                    verificationCodeEntity.setCreated(Instant.now());
                    verificationCodeEntity.setLastModified(verificationCodeEntity.getCreated());

                    return updateVerificationCode(verificationCodeEntity);
                }
            } else {
                log.debug("address verified - recipientId: {} - channelType: {}", recipientId, channelType);
                return updateAddressBook(true
                        ,recipientId
                        ,senderId
                        ,legalType
                        ,channelType
                        ,address
                        ,verificationCode);

            }
        }, (r,r1) -> r1 instanceof VerificationCodeEntity);
    }
*/
/*
    public Mono<Boolean> saveAddressBookEx1(String recipientId,
                                           String senderId,
                                           String legalType,
                                           String channelType,
                                           String address,
                                           final String verificationCode) {

        return getVerifiedAddress(recipientId, channelType, address).zipWhen(r -> {
            if (r.getPk() == null) {
                log.debug("address not verified - recipientId: {} - channelType: {}", recipientId, channelType);

                if (verificationCode != null && !verificationCode.isEmpty()) {
                    log.debug("verificationCode is not empty");
                    return getVerificationCode(recipientId, channelType, address).zipWhen(xr -> {
                        if (xr.getPk() == null) {
                            log.debug("VerificationCodeEntity not found");
                            throw new PnVerificationCodeInvalid();
                        } else {
                            log.debug("VerificationCodeEntity found");

                            if (xr.getVerificationCode().equals(verificationCode)) {
                                log.debug("verificationCode matched");

                                return updateAddressBook(false
                                        ,recipientId
                                        ,senderId
                                        ,legalType
                                        ,channelType
                                        ,address
                                        ,verificationCode);

                            } else {
                                log.debug("verificationCode NOT matched");
                                throw new PnVerificationCodeInvalid();
                            }
                        }
                    }, (xr, xr1) -> xr1);

                } else {
                    log.debug("verificationCode is empty");
                    // genera provvisoriamente un codice di verifica e scrive su VerificationCodeTable
                    String newVerificationCode = getNewVerificationCode();

                    VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity();
                    verificationCodeEntity.setPk(VerificationCodeEntity.getPk(recipientId, channelType, address));
                    verificationCodeEntity.setSk(VerificationCodeEntity.SK_VALUE);
                    verificationCodeEntity.setVerificationCode(newVerificationCode);
                    verificationCodeEntity.setCreated(Instant.now());
                    verificationCodeEntity.setLastModified(verificationCodeEntity.getCreated());

                    return updateVerificationCode(verificationCodeEntity);
                }
            } else {
                log.debug("address verified - recipientId: {} - channelType: {}", recipientId, channelType);
                return updateAddressBook(true
                        ,recipientId
                        ,senderId
                        ,legalType
                        ,channelType
                        ,address
                        ,verificationCode);

            }
        }, (r,r1) -> r1 instanceof VerificationCodeEntity);
    }*/


    // Inizio metodi privati

    private Mono<AddressBookEntity> updateAddressBook(boolean verified,
                                                      String recipientId,
                                                      String senderId,
                                                      String legalType,
                                                      String channelType,
                                                      String address,
                                                      String verificationCode )
    {
        /*VerifiedAddressEntity vae = null;
        TransactWriteItemsEnhancedRequest transaction = null;
        UpdateItemEnhancedRequest<VerifiedAddressEntity> req1 = null;
        UpdateItemEnhancedRequest<AddressBookEntity> req2 = null;

        if (!verified) {
            vae = new VerifiedAddressEntity();
            vae.setPk(VerifiedAddressEntity.getPk(recipientId, channelType, address));
            vae.setSk(VerifiedAddressEntity.SK_VALUE);
            vae.setCreated(Instant.now());
            vae.setLastModified(Instant.now());

            req1 = UpdateItemEnhancedRequest
                    .builder(VerifiedAddressEntity.class)
                    .item(vae)
                    .ignoreNulls(true)
                    .build();
        }

        AddressBookEntity ab = new AddressBookEntity();
        ab.setAddress(address);
        ab.setCreated(Instant.now());
        ab.setLastModified(Instant.now());
        ab.setPk(AddressBookEntity.getPk(recipientId));
        ab.setSk(AddressBookEntity.getSk(legalType,senderId, channelType));
        ab.setVerificationCode(verificationCode);

        req2 = UpdateItemEnhancedRequest
                .builder(AddressBookEntity.class)
                .item(ab)
                .ignoreNulls(true)
                .build();

        if (vae != null) {
            transaction = TransactWriteItemsEnhancedRequest
                    .builder()
                    .addUpdateItem(verifiedAddressTable, req1)
                    .addUpdateItem(addressBookTable, req2)
                    .build();
        } else {
            transaction = TransactWriteItemsEnhancedRequest
                    .builder()
                    .addUpdateItem(addressBookTable, req2)
                    .build();
        }


        return Mono.fromFuture(dynamoDbEnhancedAsyncClient.transactWriteItems(transaction).thenApply(x -> ab));*/
        return null;
    }


    private Mono<VerificationCodeEntity> updateVerificationCode(VerificationCodeEntity verificationCode) {
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(verificationCode.getPk(), verificationCode.getSk()))
                .build();

        return  Mono.fromFuture(verificationCodeTable.getItem(getReq).thenCompose(r -> {
            if (r != null) {
                // update -> don't modify created
                verificationCode.setCreated(null);
            }
            else
                // create -> don't set lastModified
                verificationCode.setLastModified(null);

            UpdateItemEnhancedRequest<VerificationCodeEntity> updRequest = UpdateItemEnhancedRequest.builder(VerificationCodeEntity.class)
                    .item(verificationCode)
                    .ignoreNulls(true)
                    .build();
            return verificationCodeTable.updateItem(updRequest);
        }));
    }
/*
    private Mono<VerifiedAddressEntity> getVerifiedAddress(String recipientId, String channelType, String addressHash) {
        String pk = VerifiedAddressEntity.getPk(recipientId, channelType, addressHash);
        String sk = VerifiedAddressEntity.SK_VALUE;
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .build();

        return  Mono.fromFuture(verifiedAddressTable.getItem(getReq).thenApply(r -> {
            if (r != null) {
                log.debug("getVerifiedAddress found");
            }
            else {
                log.debug("getVerifiedAddress Not found");
                r = new VerifiedAddressEntity();
            }

            return r;
        }));
    }

    private Mono<VerificationCodeEntity> getVerificationCode(String recipientId, String channelType, String addressHash) {
        String pk = VerificationCodeEntity.getPk(recipientId, channelType, addressHash);
        String sk = VerificationCodeEntity.SK_VALUE;
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .build();

        return  Mono.fromFuture(verificationCodeTable.getItem(getReq).thenApply(r -> {
            if (r != null) {
                log.debug("getVerificationCode found");
            }
            else {
                log.debug("getVerificationCode Not found");
                r = new VerificationCodeEntity();
            }

            return r;
        }));


    }
*/
}
