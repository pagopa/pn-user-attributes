package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConsentEntity {
    private static final String PK_PREFIX = "CO#";

    public static String getPk(String recipientId) {
        return PK_PREFIX + recipientId;
    }

    public String getRecipientIdNoPrefix() {
        return this.recipientId.substring(PK_PREFIX.length());
    }

    private String recipientId;
    private String consentType;

    private Instant created;
    private Instant lastModified;

    private boolean accepted;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    @DynamoDbAttribute("created")
    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    @DynamoDbAttribute("lastModified")
    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    @DynamoDbAttribute("accepted")
    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
