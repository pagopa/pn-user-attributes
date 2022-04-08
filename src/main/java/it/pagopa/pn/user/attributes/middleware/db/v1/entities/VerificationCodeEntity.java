package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import it.pagopa.pn.user.attributes.utils.EncodingUtils;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * pk: VC#<rcptId>#<chType>#<addressHash>
 * sk: null
 */
@DynamoDbBean
@Data
public class VerificationCodeEntity {
    public final static String SK_VALUE = "*";
    private static final String PK_PREFIX = "VC#";
    private static final String PK_ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;
    private static final int PK_ITEMS_CHANNEL_TYPE = 2;
    private static final int PK_ITEMS_HASHED_ADDRESS = 3;

    /**
     * Return an array of Strings from pk
     * @param pk
     *  example: VC#123e4567-e89b-12d3-a456-426614174000#EMAIL#bm9tZS5jb2dub21lQGRvbWluaW8uaXQ=
     *  ret.get(1) = "123e4567-e89b-12d3-a456-426614174000"     recipientId
     *  ret.get(2) = "EMAIL"    channelType
     *  ret.get(3) = "bm9tZS5jb2dub21lQGRvbWluaW8uaXQ="     hashed address   (decoded: nome.cognome@dominio.it)
     * @return List<String> ret
     */
    private static List<String> getPkSplitParts(String pk) {
        return new ArrayList<String>(Arrays.asList(pk.split(PK_ITEMS_SEPARATOR)));
    }

    public static String getPk(String recipientId, String channelType, String address) {
        return PK_PREFIX + recipientId
                + PK_ITEMS_SEPARATOR + channelType
                + PK_ITEMS_SEPARATOR + EncodingUtils.base64Encoding(address);
    }


    public String getRecipientId() {
        return getPkSplitParts(pk).get(PK_ITEMS_RECIPIENTID);
    }
    public String getChannelType() {
        return getPkSplitParts(pk).get(PK_ITEMS_CHANNEL_TYPE);
    }
    public String getHashedAddress() {
        return getPkSplitParts(pk).get(PK_ITEMS_HASHED_ADDRESS);
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("pk")}))  private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute("sk")}))  private String sk;

    @Getter(onMethod=@__({@DynamoDbAttribute("created")}))  private String created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private String lastModified;

    @Getter(onMethod=@__({@DynamoDbAttribute("validationCode")}))  private String validationCode;
}
