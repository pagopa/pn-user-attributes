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

    public String generatePecBody(String verificationCode) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        templateModel.put(FIELD_VERIFICATION_CODE, verificationCode);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_VERIFICATION_TEMPLATE,
                templateModel
        );
    }

    public String generateEmailBody(String verificationCode) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        templateModel.put(FIELD_VERIFICATION_CODE, verificationCode);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.EMAIL_VERIFICATION_TEMPLATE,
                templateModel
        );
    }

    public String generatePecConfirmBody() {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_CONFIRM_TEMPLATE,
                templateModel
        );
    }

    public String generatePecRejectBody() {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);
        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.PEC_REJECT_TEMPLATE,
                templateModel
        );
    }

    @Override
    public String generateEmailSubject() {
        return pnUserattributesConfig.getVerificationCodeMessageEMAILSubject();
    }

    @Override
    public String generatePecSubject() {
        return pnUserattributesConfig.getVerificationCodeMessagePECSubject();
    }

    @Override
    public String generatePecSubjectConfirm() {
        return pnUserattributesConfig.getVerificationCodeMessagePECConfirmSubject();
    }

    @Override
    public String generatePecSubjectReject() {
        return pnUserattributesConfig.getVerificationCodeMessagePECRejectSubject();
    }

    @Override
    public String generateSmsBody() {
        return pnUserattributesConfig.getVerificationCodeMessageSMS();
    }

    private static String readLocalImagesInBase64(String classPath) {
        try (InputStream ioStream = new ClassPathResource(classPath).getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(ioStream);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new PnInternalException("error during file conversion", PnUserattributesExceptionCodes.ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE, e);
        }
    }
}
