package it.pagopa.pn.user.attributes.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConsentEntity extends BaseEntity {
    private static final String PK_PREFIX = "CO#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;

    public static final String COL_ACCEPTED = "accepted";

    public ConsentEntity(String recipientId, String consentType){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(consentType);
    }

    @DynamoDbIgnore
    public String getRecipientId() {
        return getPk().split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }

    @DynamoDbIgnore
    public String getConsentType() {
        return getSk();
    }


    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ACCEPTED)}))  private boolean accepted;
}
