package it.pagopa.pn.user.attributes.middleware.templates.impl;

import com.amazonaws.util.IOUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes;
import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.utils.DocumentComposition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@AllArgsConstructor
public class TemplateGeneratorByDocComposition implements TemplateGenerator {

    public static final String FIELD_VERIFICATION_CODE = "verificationCode";
    public static final String FIELD_LOGO = "logoBase64";
    private static final String TEMPLATES_DIR_NAME = "verificationcodemessages";
    private static final String SEND_LOGO_BASE64 = readLocalImagesInBase64(TEMPLATES_DIR_NAME + "/images/aar-logo-short-small.png");

    private final DocumentComposition documentComposition;
    private final PnUserattributesConfig pnUserattributesConfig;

    /**
     * Generates the PEC body.
     * <p>
     * It constructs a map with the provided verification code and the logo.
     * The template is processed using the DocumentComposition service, which
     * executes the specified text template.
     * </p>
     *
     * @param verificationCode the verification code to include in the PEC body.
     * @return the generated PEC body as a String.
     */
    public String generatePecBody(String verificationCode) {
        log.debug("retrieve DocComposition template PecBody");
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        templateModel.put(FIELD_VERIFICATION_CODE, verificationCode);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_VERIFICATION_TEMPLATE,
                templateModel
        );
    }

    /**
     * Generates the email body.
     * <p>
     * It constructs a map with the provided verification code and the logo.
     * The template is processed using the DocumentComposition service, which
     * executes the specified text template.
     * </p>
     *
     * @param verificationCode the code to include in the email body for verification purposes.
     * @return the generated email body template as a String.
     */
    public String generateEmailBody(String verificationCode) {
        log.debug("retrieve DocComposition template EmailBody");
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        templateModel.put(FIELD_VERIFICATION_CODE, verificationCode);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.EMAIL_VERIFICATION_TEMPLATE,
                templateModel
        );
    }

    /**
     * Generates the confirmation body for a PEC message.
     * <p>
     * It constructs a map with the provided verification code and the logo.
     * The template is processed using the DocumentComposition service, which
     * executes the specified text template.
     * </p>
     *
     * @return the generated PEC confirmation body as a String.
     */
    public String generatePecConfirmBody() {
        log.debug("retrieve DocComposition template PecConfirmBody");
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_CONFIRM_TEMPLATE,
                templateModel
        );
    }

    /**
     * Generates the rejection body for a PEC message.
     * <p>
     * <p>
     * It constructs a map with the logo then the template is processed using the DocumentComposition service, which
     * executes the specified text template.
     * </p>
     *
     * @return the generated PEC rejection body as a String.
     */
    public String generatePecRejectBody() {
        log.debug("retrieve DocComposition template PecRejectBody");
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_REJECT_TEMPLATE,
                templateModel
        );
    }

    /**
     * Generates the email subject.
     * <p>
     * It retrieves the email subject template.
     * </p>
     *
     * @return the generated email subject template as a String.
     */
    @Override
    public String generateEmailSubject() {
        log.debug("retrieve DocComposition template EmailSubject");
        return pnUserattributesConfig.getVerificationCodeMessageEMAILSubject();
    }

    /**
     * Generates the PEC subject.
     * <p>
     * It retrieves the subject of a PEC message.
     * </p>
     *
     * @return the generated PEC subject template as a String.
     */
    @Override
    public String generatePecSubject() {
        log.debug("retrieve DocComposition template VerificationCodeMessagePECSubject");
        return pnUserattributesConfig.getVerificationCodeMessagePECSubject();
    }

    /**
     * Generates the PEC confirmation subject.
     * <p>
     * It retrieves the subject confirmation of a PEC message.
     * </p>
     *
     * @return the generated PEC confirmation subject template as a String.
     */
    @Override
    public String generatePecSubjectConfirm() {
        log.debug("retrieve DocComposition template VerificationCodeMessagePECConfirmSubject");
        return pnUserattributesConfig.getVerificationCodeMessagePECConfirmSubject();
    }

    /**
     * Generates the PEC reject subject.
     * <p>
     * It retrieves the subject reject of a PEC message.
     * </p>
     *
     * @return the generated PEC reject subject template as a String.
     */
    @Override
    public String generatePecSubjectReject() {
        log.debug("retrieve DocComposition template VerificationCodeMessagePECRejectSubject");
        return pnUserattributesConfig.getVerificationCodeMessagePECRejectSubject();
    }

    /**
     * Generates the SMS body.
     * <p>
     * It retrieves the SMS body of message.
     * </p>
     *
     * @return the generated SMS body template as a String.
     */
    @Override
    public String generateSmsBody() {
        log.debug("retrieve DocComposition template VerificationCodeMessageSMS");
        return pnUserattributesConfig.getVerificationCodeMessageSMS();
    }

    /**
     * Reads a local image from the classpath and converts it to a Base64 string.
     * <p>
     * It retrieves an image file from the specified classpath location,
     * reads its binary content, and encodes it as a Base64 string.
     * </p>
     *
     * @param classPath the classpath location of the image file to be read.
     * @return the Base64-encoded representation of the image as a String.
     * @throws PnInternalException if an error occurs during the file reading or conversion process.
     */
    private static String readLocalImagesInBase64(String classPath) {
        try (InputStream ioStream = new ClassPathResource(classPath).getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(ioStream);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new PnInternalException("error during file conversion", PnUserattributesExceptionCodes.ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE, e);
        }
    }
}
