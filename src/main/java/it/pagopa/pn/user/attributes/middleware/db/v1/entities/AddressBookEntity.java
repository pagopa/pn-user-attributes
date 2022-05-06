package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = true)
public class AddressBookEntity extends BaseEntity {


    private static final String PK_PREFIX = "AB#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final String SENDER_ID_DEFAULT = "default";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int SK_ITEMS_ADDRESS_TYPE = 0;
    private static final int SK_ITEMS_SENDER_ID = 1;
    private static final int SK_ITEMS_CHANNEL_TYPE = 2;

    public static final String COL_ADDRESS_ID = "addressId";

    public AddressBookEntity(String recipientId, String addressType, String senderId, String channelType){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(addressType + ITEMS_SEPARATOR + (senderId==null?SENDER_ID_DEFAULT:senderId) + ITEMS_SEPARATOR + (channelType==null?"":channelType));
    }

    @DynamoDbIgnore
    public String getRecipientId() {
        return getPk().split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }

    @DynamoDbIgnore
    public String getChannelType() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_CHANNEL_TYPE];
    }
    @DynamoDbIgnore
    public String getAddressType() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_ADDRESS_TYPE];
    }
    @DynamoDbIgnore
    public String getSenderId() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_SENDER_ID];
    }


    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ADDRESS_ID)}))  private String addressId;
}
