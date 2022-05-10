package it.pagopa.pn.user.attributes.middleware.db.v1.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//@SpringBootTest
//@TestPropertySource("/application.properties")
class DynamoDBConfigTest {

    //@Value(DynamoDBConfig.AWS_REGION_CODE)
    //private String awsRegion;
    //@Value(DynamoDBConfig.AWS_ENDPOINT_URL)
    //private String dynamoDBEndpoint;

    private AwsServicesClientsConfig dbConfig;

    @BeforeEach
    void setUp() {
        AwsConfigs awsCfg = new AwsConfigs();
        awsCfg.setEndpointUrl("");
        awsCfg.setRegionCode("");
        dbConfig = new AwsServicesClientsConfig( awsCfg );
    }

    @Test
    void dynamoDbAsyncClient() {
        DynamoDbAsyncClient client = dbConfig.dynamoDbAsyncClient();
        assertNotNull(client);
    }

    @Test
    void getDynamoDbEnhancedAsyncClient() {
        DynamoDbEnhancedAsyncClient client = dbConfig.dynamoDbEnhancedAsyncClient( dbConfig.dynamoDbAsyncClient() );
        assertNotNull(client);
    }
}