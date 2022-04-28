package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressNotFound;
import it.pagopa.pn.user.attributes.exceptions.PnDigitalAddressesNotFound;
import it.pagopa.pn.user.attributes.exceptions.PnVerificationCodeInvalid;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerifiedAddressEntity;
import it.pagopa.pn.user.attributes.services.v1.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.user.attributes.exceptions.RestWebExceptionHandler.findExceptionRootCause;

@Repository
@Slf4j
public class AddressBookDao extends BaseDao {
    private static final String DYNAMODB_TABLE_NAME = "${pn.user-attributes.dynamodb.table-name}";

    DynamoDbAsyncTable<AddressBookEntity> addressBookTable;
    DynamoDbAsyncTable<VerificationCodeEntity> verificationCodeTable;
    DynamoDbAsyncTable<VerifiedAddressEntity> verifiedAddressTable;
    DynamoDbAsyncClient dynamoDbAsyncClient;
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    String table;

    public AddressBookDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                          DynamoDbAsyncClient dynamoDbAsyncClient,
                          @Value(DYNAMODB_TABLE_NAME) String table
                          ) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.addressBookTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(AddressBookEntity.class));
        this.verificationCodeTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(VerificationCodeEntity.class));
        this.verifiedAddressTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(VerifiedAddressEntity.class));
        this.table = table;
    }

    // Crea o modifica l'entity VerificationCodeEntity

    public Mono<Object> deleteAddressBook(String recipientId, String senderId, boolean isLegal, String channelType) throws RuntimeException {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = AddressBookEntity.getSk(isLegal?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue(),
                senderId,
                channelType);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk",  AttributeValue.builder().s(pk).build());

        Expression exp = Expression.builder()
                .expression(AddressBookEntity.COL_PK + " = :pk")
                .expressionValues(expressionValues)
                .build();

        DeleteItemEnhancedRequest delRequest = DeleteItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .conditionExpression(exp)
                .build();


        return Mono.fromFuture(addressBookTable.deleteItem(delRequest)
                .exceptionally(throwable -> {
                    Throwable rootCause = findExceptionRootCause(throwable);
                    if (rootCause instanceof ConditionalCheckFailedException)
                        throw new PnDigitalAddressNotFound();
                    else
                        try {
                            throw throwable;
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                }));

    }


    public Flux<AddressBookEntity> getCourtesyAddressBySender(String recipientId, String senderId) {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = AddressBookEntity.getSk(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue(),
                                      senderId,
                                       null);

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(pk, sk)))
                .scanIndexForward(true)
                .build();

        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));
    }

    public Flux<AddressBookEntity> getCourtesyAddressByRecipient(String recipientId) {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = AddressBookEntity.getSk(CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue(),
                null,
                null);

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(pk, sk)))
                .scanIndexForward(true)
                .build();

        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));
    }

    public Flux<AddressBookEntity> getLegalAddressBySender(String recipientId, String senderId) {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = AddressBookEntity.getSk(LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue(),
                senderId,
                null);

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(pk, sk)))
                .scanIndexForward(true)
                .build();

        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));

    }

    public Flux<AddressBookEntity> getLegalAddressByRecipient(String recipientId) {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = AddressBookEntity.getSk(LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue(),
                null,
                null);

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(pk, sk)))
                .scanIndexForward(true)
                .build();

        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));

    }

    public Flux<AddressBookEntity> getAddressesByRecipient(String recipientId) {
        String pk = AddressBookEntity.getPk(recipientId);
        String sk = null;

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(pk, sk)))
                .scanIndexForward(true)
                .build();


        return Flux.from(addressBookTable.query(qeRequest)
                .items())
                .switchIfEmpty(Mono.error(new PnDigitalAddressesNotFound()));

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

    public Mono<Boolean> saveAddressBookEx(String recipientId,
                                           String senderId,
                                           boolean isLegal,
                                           String channelType,
                                           String address,
                                           final String verificationCode) {
        return getVerifiedAddress(recipientId, channelType, address).zipWhen(r -> {
            if (r.getPk() == null) {
                log.debug("address not verified - recipientId: {} - channelType: {}", recipientId, channelType);

                if (verificationCode != null && !verificationCode.isEmpty()) {
                    log.debug("verificationCode is not empty");
                    ///
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
                                                         ,isLegal
                                                         ,channelType
                                                         ,address
                                                         ,verificationCode);

                            } else {
                                log.debug("verificationCode NOT matched");
                                throw new PnVerificationCodeInvalid();
                            }
                        }
                    }, (xr, xr1) -> {
//                        if (xr != null) {
//                            // Address not verified
//                            return true;
//                        } else
//                            // scrivi su VerifiedAddress
//                            return false;
                        return xr1;
                    });
                    ///

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
                        ,isLegal
                        ,channelType
                        ,address
                        ,verificationCode);

            }
        }, (r,r1) -> {
            if (r1 instanceof VerificationCodeEntity) {
                // Address not verified
                return true;
            } else
                // Address verified
                return false;
        });
    }


    // Inizio metodi privati

    private Mono<AddressBookEntity> updateAddressBook(boolean verified,
                                                      String recipientId,
                                                      String senderId,
                                                      boolean isLegal,
                                                      String channelType,
                                                      String address,
                                                      String verificationCode )
    {
        VerifiedAddressEntity vae = null;
        TransactWriteItemsEnhancedRequest transaction = null;
        UpdateItemEnhancedRequest req1 = null;
        UpdateItemEnhancedRequest req2 = null;

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
        ab.setSk(AddressBookEntity.getSk(isLegal? LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue() : CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue()
                ,senderId, channelType));
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


        return Mono.fromFuture(dynamoDbEnhancedAsyncClient.transactWriteItems(transaction).thenApply(x -> ab));
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

    private String getNewVerificationCode() {
        log.info("generated a new verificationCode: {}", AddressBookService.VERIFICATION_CODE_OK);
        return AddressBookService.VERIFICATION_CODE_OK;
    }

}
