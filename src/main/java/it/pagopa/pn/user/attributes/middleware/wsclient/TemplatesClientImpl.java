package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@CustomLog
@Component
public class TemplatesClientImpl {

    private final TemplateApi templateEngineClient;

    public String mailVerificationCodeBody(LanguageEnum xLanguage, MailVerificationCodeBody mailVerificationCodeBody) {
        return templateEngineClient.mailVerificationCodeBody(xLanguage, mailVerificationCodeBody);
    }

    public String mailVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.mailVerificationCodeSubject(xLanguage);
    }

    public String pecVerificationCodeBody(LanguageEnum xLanguage, PecVerificationCodeBody pecVerificationCodeBody) {
        return templateEngineClient.pecVerificationCodeBody(xLanguage, pecVerificationCodeBody);
    }

    public String pecVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecVerificationCodeSubject(xLanguage);
    }

    public String pecValidationContactsSuccessBody(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsSuccessBody(xLanguage);
    }

    public String pecValidationContactsSuccessSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsSuccessSubject(xLanguage);
    }

    public String pecValidationContactsRejectBody(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsRejectBody(xLanguage);
    }

    public String pecValidationContactsRejectSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsRejectSubject(xLanguage);
    }

    public String smsVerificationCodeBody(LanguageEnum xLanguage) {
        return templateEngineClient.smsVerificationCodeBody(xLanguage);
    }
}
