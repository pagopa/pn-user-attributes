package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Data
public class ConsentEntity {
    private static final String PK_PREFIX = "CO#";

    public static String getPk(String recipientId) {
        return PK_PREFIX + recipientId;
    }

    public String getRecipientIdNoPrefix() {
        return this.recipientId.substring(PK_PREFIX.length());
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("pk")}))  private String recipientId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute("sk")}))  private String consentType;

    @Getter(onMethod=@__({@DynamoDbAttribute("created")}))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastModified")}))  private Instant lastModified;

    @Getter(onMethod=@__({@DynamoDbAttribute("accepted")}))  private boolean accepted;
}
