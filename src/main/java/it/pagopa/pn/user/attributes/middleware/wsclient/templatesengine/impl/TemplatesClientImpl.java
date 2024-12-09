package it.pagopa.pn.user.attributes.middleware.wsclient.templatesengine.impl;

import it.pagopa.pn.user.attributes.middleware.wsclient.templatesengine.TemplatesClient;
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
public class TemplatesClientImpl implements TemplatesClient {

    private final TemplateApi templateEngineClient;

    @Override
    public String mailVerificationCodeBody(LanguageEnum xLanguage, MailVerificationCodeBody mailVerificationCodeBody) {
        return templateEngineClient.mailVerificationCodeBody(xLanguage, mailVerificationCodeBody);
    }

    @Override
    public String mailVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.mailVerificationCodeSubject(xLanguage);
    }

    @Override
    public String pecVerificationCodeBody(LanguageEnum xLanguage, PecVerificationCodeBody pecVerificationCodeBody) {
        return templateEngineClient.pecVerificationCodeBody(xLanguage, pecVerificationCodeBody);
    }

    @Override
    public String pecVerificationCodeSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecVerificationCodeSubject(xLanguage);
    }

    @Override
    public String pecValidationContactsSuccessBody(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsSuccessBody(xLanguage);
    }

    @Override
    public String pecValidationContactsSuccessSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsSuccessSubject(xLanguage);
    }

    @Override
    public String pecValidationContactsRejectBody(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsRejectBody(xLanguage);
    }

    @Override
    public String pecValidationContactsRejectSubject(LanguageEnum xLanguage) {
        return templateEngineClient.pecValidationContactsRejectSubject(xLanguage);
    }

    @Override
    public String smsVerificationCodeBody(LanguageEnum xLanguage) {
        return templateEngineClient.smsVerificationCodeBody(xLanguage);
    }
}
