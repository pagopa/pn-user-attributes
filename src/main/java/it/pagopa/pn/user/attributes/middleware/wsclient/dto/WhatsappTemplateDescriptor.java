package it.pagopa.pn.user.attributes.middleware.wsclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.util.List;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE;

/**
 * Descrittore del template WhatsApp restituito da pn-templates-engine (WhatsApp_VerificationCodeBody).
 * Il testo è approvato su Meta: SEND invia solo nome, lingua e parametri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappTemplateDescriptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** Segnaposto del codice OTP nel template (come in SMS_VerificationCodeBody). */
    public static final String CODE_PLACEHOLDER = "%s";

    private String templateName;
    private String language;
    @ToString.Exclude
    private List<String> bodyParameters;
    @ToString.Exclude
    private List<String> buttonParameters;
    @ToString.Exclude
    private String previewText;

    /**
     * Sostituisce il segnaposto del codice e interpreta il descrittore.
     * Si usa {@link String#replace} invece di {@link String#format} perché il segnaposto compare più volte.
     */
    public static WhatsappTemplateDescriptor fromVerificationCodeTemplate(String template, String verificationCode) {
        if (!StringUtils.hasText(template)) {
            throw new PnInternalException("WhatsApp verification code template not found", ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE);
        }
        try {
            WhatsappTemplateDescriptor descriptor = OBJECT_MAPPER.readValue(template.replace(CODE_PLACEHOLDER, verificationCode), WhatsappTemplateDescriptor.class);
            if (!StringUtils.hasText(descriptor.getTemplateName()) || !StringUtils.hasText(descriptor.getLanguage())) {
                throw new PnInternalException("WhatsApp template descriptor without templateName or language", ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE);
            }
            return descriptor;
        } catch (PnInternalException e) {
            throw e;
        } catch (Exception e) {
            throw new PnInternalException("Invalid WhatsApp template descriptor", ERROR_CODE_BADCONFIGURATION_MISSING_TEMPLATE, e);
        }
    }
}
