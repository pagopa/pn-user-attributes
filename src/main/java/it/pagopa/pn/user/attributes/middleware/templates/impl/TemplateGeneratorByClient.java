package it.pagopa.pn.user.attributes.middleware.templates.impl;

import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.middleware.wsclient.TemplatesClientImpl;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;

public record TemplateGeneratorByClient(TemplatesClientImpl templatesClient) implements TemplateGenerator {

    @Override
    public String generateEmailBody(String verificationCode) {
        MailVerificationCodeBody mailVerificationCodeBody = new MailVerificationCodeBody();
        mailVerificationCodeBody.setVerificationCode(verificationCode);
        return templatesClient.mailVerificationCodeBody(LanguageEnum.IT, mailVerificationCodeBody);
    }

    @Override
    public String generateEmailSubject() {
        return templatesClient.mailVerificationCodeSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecBody(String verificationCode) {
        PecVerificationCodeBody pecVerificationCodeBody = new PecVerificationCodeBody();
        pecVerificationCodeBody.setVerificationCode(verificationCode);
        return templatesClient.pecVerificationCodeBody(LanguageEnum.IT, pecVerificationCodeBody);
    }

    @Override
    public String generatePecConfirmBody() {
        return templatesClient.pecValidationContactsSuccessBody(LanguageEnum.IT);
    }

    @Override
    public String generatePecRejectBody() {
        return templatesClient.pecValidationContactsRejectBody(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubject() {
        return templatesClient.pecVerificationCodeSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubjectConfirm() {
        return templatesClient.pecValidationContactsSuccessSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubjectReject() {
        return templatesClient.pecValidationContactsRejectSubject(LanguageEnum.IT);
    }

    @Override
    public String generateSmsBody() {
        return templatesClient.smsVerificationCodeBody(LanguageEnum.IT);
    }
}
