package it.pagopa.pn.user.attributes.middleware.db.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("aws")
public class AwsConfigs {

    private String dynamodbTable;
    private String dynamodbTableHistory;
}