package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;


/**
 * pk: VC#<rcptId>#<chType>#<addressHash>
 * sk: null
 */
@DynamoDbBean
@Data
public class VerificationCodeEntity {
    private static final String PK_PREFIX = "VC#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int SK_ITEMS_HASHED_ADDRESS = 0;
    private static final int SK_ITEMS_CHANNEL_TYPE = 1;


    public VerificationCodeEntity(String recipientId, String address, String channelType){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(address + ITEMS_SEPARATOR + (channelType==null?"":channelType));
    }

    @DynamoDbIgnore
    public String getRecipientId() {
        return pk.split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }
    @DynamoDbIgnore
    public String getChannelType() {
        return sk.split(ITEMS_SEPARATOR)[SK_ITEMS_CHANNEL_TYPE];
    }
    @DynamoDbIgnore
    public String getHashedAddress() {
        return sk.split(ITEMS_SEPARATOR)[SK_ITEMS_HASHED_ADDRESS];
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("pk")}))  private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute("sk")}))  private String sk;

    @Getter(onMethod=@__({@DynamoDbAttribute("created")}))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private Instant lastModified;

    @Getter(onMethod=@__({@DynamoDbAttribute("verificationCode")}))  private String verificationCode;

    @Getter(onMethod=@__({@DynamoDbAttribute("ttl")}))  private long ttl;
}
