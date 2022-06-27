package it.pagopa.pn.user.attributes.middleware.db.entities;

import lombok.*;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConsentEntity extends BaseEntity {
    private static final String PK_PREFIX = "CO#";
    private static final String ITEMS_SEPARATOR = "#";
    private static final int PK_ITEMS_RECIPIENTID = 1;

    private static final int SK_ITEMS_CONSENTTYPE = 0;
    private static final int SK_ITEMS_VERSION = 1;

    public static final String DEFAULT_VERSION = "V001";

    public static final String COL_ACCEPTED = "accepted";

    public ConsentEntity(String recipientId, String consentType, String version){
        this.setPk(PK_PREFIX + recipientId);
        this.setSk(consentType + ITEMS_SEPARATOR + (StringUtils.hasText(version)?version:DEFAULT_VERSION));
    }

    @DynamoDbIgnore
    public String getRecipientId() {
        return getPk().split(ITEMS_SEPARATOR)[PK_ITEMS_RECIPIENTID];
    }

    @DynamoDbIgnore
    public String getConsentType() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_CONSENTTYPE];
    }

    @DynamoDbIgnore
    public String getConsentVersion() {
        return getSk().split(ITEMS_SEPARATOR)[SK_ITEMS_VERSION];
    }

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ACCEPTED)}))  private boolean accepted;
}
