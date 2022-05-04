package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Data
@NoArgsConstructor
public class ConsentEntity {
    private static final String PK_PREFIX = "CO#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;

    private static final String COL_PK = "pk";
    private static final String COL_SK = "sk";


    public ConsentEntity(String recipientId, String consentType){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(consentType);
    }

    @DynamoDbIgnore
    public String getRecipientId() {
        return pk.split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }

    @DynamoDbIgnore
    public String getConsentType() {
        return sk;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("pk")}))  private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute("sk")}))  private String sk;

    @Getter(onMethod=@__({@DynamoDbAttribute("accepted")}))  private boolean accepted;

    @Getter(onMethod=@__({@DynamoDbAttribute("created")}))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private Instant lastModified;

}
