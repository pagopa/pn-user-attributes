package it.pagopa.pn.user.attributes.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.user-attributes")
@NoArgsConstructor
@Slf4j
@Import(SharedAutoConfiguration.class)
public class PnUserattributesConfig {

    private String dynamodbTableName;

    private String clientDatavaultBasepath;
    private String clientDeliveryBasepath;
    private String clientExternalregistryBasepath;
    private String clientExternalchannelsBasepath;


    private String clientExternalchannelsHeaderExtchCxId;
    private String clientExternalchannelsSenderEmail;
    private String clientExternalchannelsSenderSms;
    private String clientExternalchannelsSenderPec;


    private String verificationCodeMessageSMS;
    private String verificationCodeMessageEMAIL;
    private String verificationCodeMessageEMAILSubject;
    private String verificationCodeMessagePEC;
    private String verificationCodeMessagePECSubject;

    private int ioactivationSendolderthandays;

    private int validationCodeMaxAttempts;
    private Duration verificationCodeTTL;

    private List<String> externalchannelDigitalCodesSuccess;

    private Topics topics;

    @Data
    public static class Topics {

        private String actions;

    }

    @Value("${pn.env.runtime}")
    private String envRuntime;

    @PostConstruct
    public void init(){
        this.verificationCodeMessageEMAILSubject = fetchMessage("emailsubject.txt");
        this.verificationCodeMessageEMAIL = fetchMessage("emailbody.txt");
        this.verificationCodeMessageSMS = fetchMessage("smsbody.txt");
        this.verificationCodeMessagePECSubject = fetchMessage("pecsubject.txt");
        this.verificationCodeMessagePEC = fetchMessage("pecbody.txt");

        if (isDevelopment()) {
            log.warn("DEVELOPMENT IS ACTIVE!");
        }
    }

    private String fetchMessage(String filename){
        try( InputStream in = getInputStreamFromResource(filename)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("cannot load message from resources", e);
            throw new PnInternalException("template not found", ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE);
        }
    }

    private InputStream getInputStreamFromResource(String filename) throws IOException {
        return ResourceUtils.getURL("classpath:verificationcodemessages/" + filename).openStream();
    }

    public boolean isDevelopment(){
        return envRuntime!=null && envRuntime.equals("DEVELOPMENT");
    }

}
