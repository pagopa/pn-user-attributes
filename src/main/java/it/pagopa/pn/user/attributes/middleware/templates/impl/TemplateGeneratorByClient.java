package it.pagopa.pn.user.attributes.middleware.templates.impl;

import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.middleware.wsclient.TemplatesClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record TemplateGeneratorByClient(TemplatesClient templatesClient) implements TemplateGenerator {

    @Override
    public String generateEmailBody(String verificationCode) {
        log.debug("retrieve template mailVerificationCodeBody");
        System.out.println("SYS - retrieve template mailVerificationCodeBody");
        MailVerificationCodeBody mailVerificationCodeBody = new MailVerificationCodeBody();
        mailVerificationCodeBody.setVerificationCode(verificationCode);
        return templatesClient.mailVerificationCodeBody(LanguageEnum.IT, mailVerificationCodeBody);
    }

    @Override
    public String generateEmailSubject() {
        log.debug("retrieve template mailVerificationCodeSubject");
        System.out.println("SYS - retrieve template mailVerificationCodeSubject");
        return templatesClient.mailVerificationCodeSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecBody(String verificationCode) {
        log.debug("retrieve template pecVerificationCodeBody");
        PecVerificationCodeBody pecVerificationCodeBody = new PecVerificationCodeBody();
        pecVerificationCodeBody.setVerificationCode(verificationCode);
        return templatesClient.pecVerificationCodeBody(LanguageEnum.IT, pecVerificationCodeBody);
    }

    @Override
    public String generatePecConfirmBody() {
        log.debug("retrieve template pecValidationContactsSuccessBody");
        return templatesClient.pecValidationContactsSuccessBody(LanguageEnum.IT);
    }

    @Override
    public String generatePecRejectBody() {
        log.debug("retrieve template pecValidationContactsRejectBody");
        return templatesClient.pecValidationContactsRejectBody(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubject() {
        log.debug("retrieve template pecVerificationCodeSubject");
        return templatesClient.pecVerificationCodeSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubjectConfirm() {
        log.debug("retrieve template pecValidationContactsSuccessSubject");
        return templatesClient.pecValidationContactsSuccessSubject(LanguageEnum.IT);
    }

    @Override
    public String generatePecSubjectReject() {
        log.debug("retrieve template pecValidationContactsRejectSubject");
        return templatesClient.pecValidationContactsRejectSubject(LanguageEnum.IT);
    }

    @Override
    public String generateSmsBody() {
        log.debug("retrieve template smsVerificationCodeBody");
        System.out.println("SYS - retrieve template smsVerificationCodeBody");
        return templatesClient.smsVerificationCodeBody(LanguageEnum.IT);
    }
}
