package it.pagopa.pn.user.attributes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.user-attributes")
public class PnUserattributesConfig {

    private String dynamodbTableName;
}
