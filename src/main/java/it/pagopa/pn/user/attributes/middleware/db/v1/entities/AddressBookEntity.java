package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;


/**
 * pk: AB#<rcptId>
 * sk: {ADDRESSTYPE}#{senderId}#{CHANNELTYPE}
 * ADDRESSTYPE = LEGAL, COURTESY
 * senderId = default, senderid != default
 * CHANNELTYPE = PEC, APPIO, EMAIL, SMS
 */
@DynamoDbBean
@Data
@NoArgsConstructor
public class AddressBookEntity {


    private static final String PK_PREFIX = "AB#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final String SENDER_ID_DEFAULT = "default";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int SK_ITEMS_ADDRESS_TYPE = 0;
    private static final int SK_ITEMS_SENDER_ID = 1;
    private static final int SK_ITEMS_CHANNEL_TYPE = 2;

    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";


    public AddressBookEntity(String recipientId, String addressType, String senderId, String channelType){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(addressType + ITEMS_SEPARATOR + (senderId==null?SENDER_ID_DEFAULT:senderId) + ITEMS_SEPARATOR + (channelType==null?"":channelType));
        this.setCreated(Instant.now());
        this.setLastModified(this.getCreated());
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
    public String getAddressType() {
        return sk.split(ITEMS_SEPARATOR)[SK_ITEMS_ADDRESS_TYPE];
    }

    @DynamoDbIgnore
    public String getSenderId() {
        return sk.split(ITEMS_SEPARATOR)[SK_ITEMS_SENDER_ID];
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))  private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;

    @Getter(onMethod=@__({@DynamoDbAttribute("created"), @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS) }))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private Instant lastModified;

    @Getter(onMethod=@__({@DynamoDbAttribute("addressId")}))  private String addressId;

}
