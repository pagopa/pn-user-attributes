package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecValidationContactsBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;


@AllArgsConstructor
@CustomLog
@Component
public class TemplatesClient {

    private final TemplateApi templateEngineClient;

    /**
     * Retrieves the body of the email verification code template.
     *
     * @param xLanguage the language for the template.
     * @param mailVerificationCodeBody the object containing data for the email body.
     * @return the email verification code body as a String.
     */
    public String mailVerificationCodeBody(LanguageEnum xLanguage, MailVerificationCodeBody mailVerificationCodeBody) {
        //add recipientType (si può usare direttamente il recipientId)
        return templateEngineClient.mailVerificationCodeBody(xLanguage, mailVerificationCodeBody);
    }

    /**
     * Retrieves the subject of the email verification code template.
     *
     * @param xLanguage the language for the template.
     * @return the email verification code subject as a String.
     */
    public String mailVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.mailVerificationCodeSubject(xLanguage);
    }

    /**
     * Retrieves the body of the PEC verification code template.
     *
     * @param xLanguage the language for the template.
     * @param pecVerificationCodeBody the object containing data for the PEC body.
     * @return the PEC verification code body as a String.
     */
    public String pecVerificationCodeBody(LanguageEnum xLanguage, PecVerificationCodeBody pecVerificationCodeBody) {
        return templateEngineClient.pecVerificationCodeBody(xLanguage, pecVerificationCodeBody);
    }

    /**
     * Retrieves the subject of the PEC verification code template.
     *
     * @param xLanguage the language for the template.
     * @return the PEC verification code subject as a String.
     */
    public String pecVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecVerificationCodeSubject(xLanguage);
    }

    /**
     * Retrieves the body of the PEC validation contacts success template.
     *
     * @param xLanguage the language for the template.
     * @return the PEC validation contacts success body as a String.
     */
    public String pecValidationContactsSuccessBody(LanguageEnum xLanguage, PecValidationContactsBody pecValidationContactsBody) {
        //add recipientType
        return templateEngineClient.pecValidationContactsSuccessBody(xLanguage, pecValidationContactsBody);
    }

    /**
     * Retrieves the subject of the PEC validation contacts success template.
     *
     * @param xLanguage the language for the template.
     * @return the PEC validation contacts success subject as a String.
     */
    public String pecValidationContactsSuccessSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsSuccessSubject(xLanguage);
    }

    /**
     * Retrieves the body of the PEC validation contacts rejection template.
     *
     * @param xLanguage the language for the template.
     * @return the PEC validation contacts rejection body as a String.
     */
    public String pecValidationContactsRejectBody(LanguageEnum xLanguage, PecValidationContactsBody pecValidationContactsBody) {
        //add recipientType
        return templateEngineClient.pecValidationContactsRejectBody(xLanguage, pecValidationContactsBody);
    }

    /**
     * Retrieves the subject of the PEC validation contacts rejection template.
     *
     * @param xLanguage the language for the template.
     * @return the PEC validation contacts rejection subject as a String.
     */
    public String pecValidationContactsRejectSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsRejectSubject(xLanguage);
    }

    /**
     * Retrieves the body of the SMS verification code template.
     *
     * @param xLanguage the language for the template.
     * @return the SMS verification code body as a String.
     */
    public String smsVerificationCodeBody(LanguageEnum xLanguage) {
        return templateEngineClient.smsVerificationCodeBody(xLanguage);
    }
}