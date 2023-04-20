package it.pagopa.pn.user.attributes.middleware.db.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;


/**
 * pk: VC#<rcptId>#<chType>#<addressHash>
 * sk: null
 */
@DynamoDbBean
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VerificationCodeEntity extends BaseEntity {

    public static final String GSI_INDEX_REQUESTID = "requestId-gsi";

    private static final String PK_PREFIX = "VC#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int SK_ITEMS_HASHED_ADDRESS = 0;
    private static final int SK_ITEMS_CHANNEL_TYPE = 1;

    public VerificationCodeEntity(String recipientId, String hashedaddress, String channelType){
        this.setSk(hashedaddress + ITEMS_SEPARATOR + (channelType==null?"":channelType));
        this.setPk(PK_PREFIX + recipientId);
    }

    public VerificationCodeEntity(String recipientId, String hashedaddress, String channelType, String senderId, String addressType, String realaddress){
        this(recipientId, hashedaddress, channelType);

        this.pecValid = false;
        this.codeValid = false;
        this.failedAttempts = 0;
        if (senderId == null)
            senderId = AddressBookEntity.SENDER_ID_DEFAULT;
        this.senderId = senderId;
        this.addressType = addressType;
        this.address = realaddress;
    }

    @DynamoDbIgnore
    public String getChannelType() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_CHANNEL_TYPE];
    }
    @DynamoDbIgnore
    public String getHashedAddress() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_HASHED_ADDRESS];
    }
    @DynamoDbIgnore
    public String getRecipientId() {
        return getPk().split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }


    @Getter(onMethod=@__({@DynamoDbAttribute("verificationCode")}))  private String verificationCode;

    @Getter(onMethod=@__({@DynamoDbAttribute("failedAttempts")}))  private int failedAttempts;

    @Getter(onMethod=@__({@DynamoDbAttribute("codeValid")}))  private boolean codeValid;

    @Getter(onMethod=@__({@DynamoDbAttribute("pecValid")}))  private boolean pecValid;

    @Getter(onMethod=@__({@DynamoDbSecondaryPartitionKey(indexNames = { GSI_INDEX_REQUESTID}), @DynamoDbAttribute("requestId")}))  private String requestId;

    @Getter(onMethod=@__({@DynamoDbAttribute("senderId")}))  private String senderId;

    @Getter(onMethod=@__({@DynamoDbAttribute("addressType")}))  private String addressType;

    @Getter(onMethod=@__({@DynamoDbAttribute("address")}))  private String address;

    @Getter(onMethod=@__({@DynamoDbAttribute("ttl")}))  private long ttl;
}
