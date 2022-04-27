package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * pk: AB#<rcptId>
 * sk: {ADDRESSTYPE}#{senderId}#{CHANNELTYPE}
 * ADDRESSTYPE = LEGAL, COURTESY
 * senderId = default, senderid != default
 * CHANNELTYPE = PEC, APPIO, EMAIL, SMS
 */
@DynamoDbBean
@Data
public class AddressBookEntity {
    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    private static final String PK_PREFIX = "AB#";
    public static  final  String SK_VALUE = "*";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int SK_ITEMS_ADDRESS_TYPE = 0;
    private static final int SK_ITEMS_SENDERID = 1;
    private static final int SK_ITEMS_CHANNELTYPE = 2;

    /**
     * Return an array of Strings from pk
     * @param pk
     *  example: AB#123e4567-e89b-12d3-a456-426614174000
     *  ret.get(1) = "123e4567-e89b-12d3-a456-426614174000"     recipientId
     * @return List<String> ret
     */
    private static List<String> getPkSplitParts(String pk) {
        return new ArrayList<>(Arrays.asList(pk.split(ITEMS_SEPARATOR)));
    }

    public static String getPk(String recipientId) {
        return PK_PREFIX + recipientId;
    }

    /**
     * Return an array of Strings from sk
     * @param sk
     *  example: LEGAL#default#MAIL
     *  ret.get(0) = "LEGAL"     addressType
     *  ret.get(1) = "default"   senderId
     *  ret.get(2) = "MAIL"      channelType
     * @return List<String> ret
     */
    private static List<String> getSkSplitParts(String sk) {
        return new ArrayList<>(Arrays.asList(sk.split(ITEMS_SEPARATOR)));
    }

    public static String getSk(String addressType, String senderid, String channelType) {
        String sk = null;
        if (addressType != null) {
            sk = addressType;
            if (senderid != null) {
                sk = sk + AddressBookEntity.ITEMS_SEPARATOR + senderid;
                if (channelType != null) {
                    sk = sk + AddressBookEntity.ITEMS_SEPARATOR + channelType;
                }
            }
        }

        return sk;
    }

    public String getRecipientId() {
        return getPkSplitParts(pk).get(PK_ITEMS_RECIPIENTID);
    }
    public String getAddressType() {
        return getSkSplitParts(sk).get(SK_ITEMS_ADDRESS_TYPE);
    }
    public String getSenderId() {
        return getSkSplitParts(sk).get(SK_ITEMS_SENDERID);
    }
    public String getChannelType() {
        return getSkSplitParts(sk).get(SK_ITEMS_CHANNELTYPE);
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))  private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;

    @Getter(onMethod=@__({@DynamoDbAttribute("created")}))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private Instant lastModified;

    @Getter(onMethod=@__({@DynamoDbAttribute("verificationCode")}))  private String verificationCode;

    // PROVVISORIO -> in futuro verrà eliminato il campo e salvato presso un servizio esterno
    @Getter(onMethod=@__({@DynamoDbAttribute("address")}))  private String address;
}
