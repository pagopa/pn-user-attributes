package it.pagopa.pn.user.attributes.middleware.db;

import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.concurrent.ExecutionException;

@SpringBootTest
class TestDao<T> extends BaseDao {

    DynamoDbAsyncTable<T> dbTable;

    public TestDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, String table, Class<T> typeParameter)
    {
        this.dbTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(typeParameter));
    }

    public T get(String pk, String sk) throws ExecutionException, InterruptedException {

        GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .build();

        return (T)dbTable.getItem(req).get();
    }

    public void delete(String pk, String sk) throws ExecutionException, InterruptedException {

        DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                .key(getKeyBuild(pk, sk))
                .build();

        dbTable.deleteItem(req).get();
    }


}