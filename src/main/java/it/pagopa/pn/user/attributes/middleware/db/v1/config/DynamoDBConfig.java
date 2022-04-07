package it.pagopa.pn.user.attributes.middleware.db.v1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

@Configuration
public class DynamoDBConfig {

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(
            @Value("${aws.region-code}") String awsRegion,
            @Value("${aws.endpoint-url}") String dynamoDBEndpoint) {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(dynamoDBEndpoint))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient(
        @Value("${aws.region-code}") String awsRegion,
        @Value("${aws.endpoint-url}") String dynamoDBEndpoint) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient(awsRegion, dynamoDBEndpoint))
                .build();
    }
}
