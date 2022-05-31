package it.pagopa.pn.user.attributes.config;

import it.pagopa.pn.user.attributes.exceptions.InternalErrorException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.user-attributes")
@NoArgsConstructor
@Slf4j
public class PnUserattributesConfig {

    private String dynamodbTableName;

    private String clientDatavaultBasepath;
    private String clientExternalchannelsBasepath;

    private String clientExternalchannelsHeaderExtchCxId;
    private String clientExternalchannelsSenderEmail;
    private String clientExternalchannelsSenderSms;
    private String clientExternalchannelsSenderPec;

    private String verificationCodeMessageSMS;
    private String verificationCodeMessageEMAIL;
    private String verificationCodeMessageEMAILSubject;

    @Value("${pn.env.runtime}")
    private String envRuntime;

    @PostConstruct
    public void init(){
        this.verificationCodeMessageEMAILSubject = fetchMessage("emailsubject.txt");
        this.verificationCodeMessageEMAIL = fetchMessage("emailbody.txt");
        this.verificationCodeMessageSMS = fetchMessage("smsbody.txt");

        if (isDevelopment()) {
            log.warn("DEVELOPMENT IS ACTIVE!");
        }
    }

    private String fetchMessage(String filename){
        try( InputStream in = getInputStreamFromResource(filename)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("cannot load message from resources", e);
            throw new InternalErrorException();
        }
    }

    private InputStream getInputStreamFromResource(String filename) throws IOException {
        return ResourceUtils.getURL("classpath:verificationcodemessages/" + filename).openStream();
    }

    public boolean isDevelopment(){
        return envRuntime!=null && envRuntime.equals("DEVELOPMENT");
    }

}
