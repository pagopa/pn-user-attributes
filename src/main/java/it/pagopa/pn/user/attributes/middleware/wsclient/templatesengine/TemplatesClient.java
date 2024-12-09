package it.pagopa.pn.user.attributes.middleware.wsclient.templatesengine;


import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;

public interface TemplatesClient {

    String mailVerificationCodeBody(LanguageEnum xLanguage, MailVerificationCodeBody mailVerificationCodeBody);

    String mailVerificationCodeSubject(LanguageEnum xLanguage);

    String pecVerificationCodeBody(LanguageEnum xLanguage, PecVerificationCodeBody pecVerificationCodeBody);

    String pecVerificationCodeSubject(LanguageEnum xLanguage);

    String pecValidationContactsSuccessBody(LanguageEnum xLanguage);

    String pecValidationContactsSuccessSubject(LanguageEnum xLanguage);

    String pecValidationContactsRejectBody(LanguageEnum xLanguage);

    String pecValidationContactsRejectSubject(LanguageEnum xLanguage);

    String smsVerificationCodeBody(LanguageEnum xLanguage);

}
