package it.pagopa.pn.user.attributes.middleware.db.v1.entities;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

import java.time.Instant;

public class BaseEntity {

    public static final String COL_PK = "pk";
    private static final String COL_SK = "sk";
    private static final String COL_CREATED = "created";
    private static final String COL_LAST_MODIFIED = "lastModified";

    protected BaseEntity(){
        this.setCreated(Instant.now());
        this.setLastModified(this.getCreated());
    }

    @Setter @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))  private String pk;
    @Setter @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;

    @Setter @Getter(onMethod=@__({@DynamoDbAttribute(COL_CREATED), @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)}))  private Instant created;
    @Setter @Getter(onMethod=@__({@DynamoDbAttribute(COL_LAST_MODIFIED)}))  private Instant lastModified;
}
