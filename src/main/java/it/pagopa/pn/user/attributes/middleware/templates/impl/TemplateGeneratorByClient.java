package it.pagopa.pn.user.attributes.middleware.templates.impl;

import it.pagopa.pn.user.attributes.middleware.templates.TemplateGenerator;
import it.pagopa.pn.user.attributes.middleware.wsclient.TemplatesClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.MailVerificationCodeBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecValidationContactsBody;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.PecVerificationCodeBody;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public record TemplateGeneratorByClient(TemplatesClient templatesClient) implements TemplateGenerator {

    /**
     * Generates the email body.
     * <p>
     * It constructs an email body using the provided verification code and Italian language setting, then retrieves the email template.
     * </p>
     *
     * @param verificationCode the code to include in the email body for verification purposes.
     * @return the generated email body template as a String.
     */
    @Override
    public String generateEmailBody(String verificationCode, String recipientType) {
        log.debug("retrieve template mailVerificationCodeBody");
        MailVerificationCodeBody mailVerificationCodeBody = new MailVerificationCodeBody();
        mailVerificationCodeBody.setVerificationCode(verificationCode);
        mailVerificationCodeBody.setRecipientType(recipientType);
        return templatesClient.mailVerificationCodeBody(LanguageEnum.IT, mailVerificationCodeBody);
    }

    /**
     * Generates the email subject.
     * <p>
     * It retrieves the email subject template with Italian language setting.
     * </p>
     *
     * @return the generated email subject template as a String.
     */
    @Override
    public String generateEmailSubject() {
        log.debug("retrieve template mailVerificationCodeSubject");
        return templatesClient.mailVerificationCodeSubject(LanguageEnum.IT);
    }

    /**
     * Generates the PEC body.
     * <p>
     * It constructs the body of a PEC message using the provided verification code and Italian language setting, then retrieves the PEC body.
     * </p>
     *
     * @param verificationCode the code to include in the PEC body for verification purposes.
     * @return the generated PEC body template as a String.
     */
    @Override
    public String generatePecBody(String verificationCode, String recipientType) {
        log.debug("retrieve template pecVerificationCodeBody");
        PecVerificationCodeBody pecVerificationCodeBody = new PecVerificationCodeBody();
        pecVerificationCodeBody.setVerificationCode(verificationCode);
        pecVerificationCodeBody.setRecipientType(recipientType);
        return templatesClient.pecVerificationCodeBody(LanguageEnum.IT, pecVerificationCodeBody);
    }

    /**
     * Generates the PEC confirmation body.
     * <p>
     * It retrieves the body confirmation of a PEC message with Italian language setting.
     * </p>
     *
     * @return the generated PEC confirmation body template as a String.
     */
    @Override
    public String generatePecConfirmBody(String recipientType) {
        log.debug("retrieve template pecValidationContactsSuccessBody");
        PecValidationContactsBody pecValidationContactsBody = new PecValidationContactsBody();
        pecValidationContactsBody.setRecipientType(recipientType);
        return templatesClient.pecValidationContactsSuccessBody(LanguageEnum.IT,pecValidationContactsBody);
    }

    /**
     * Generates the PEC reject body.
     * <p>
     * It retrieves the body reject of a PEC message with Italian language setting.
     * </p>
     *
     * @return the generated PEC reject body template as a String.
     */
    @Override
    public String generatePecRejectBody(String recipientType) {
        log.debug("retrieve template pecValidationContactsRejectBody");
        PecValidationContactsBody pecValidationContactsBody = new PecValidationContactsBody();
        pecValidationContactsBody.setRecipientType(recipientType);
        return templatesClient.pecValidationContactsRejectBody(LanguageEnum.IT,pecValidationContactsBody);
    }

    /**
     * Generates the PEC subject.
     * <p>
     * It retrieves the subject of a PEC message with Italian language setting.
     * </p>
     *
     * @return the generated PEC subject template as a String.
     */
    @Override
    public String generatePecSubject() {
        log.debug("retrieve template pecVerificationCodeSubject");
        return templatesClient.pecVerificationCodeSubject(LanguageEnum.IT);
    }

    /**
     * Generates the PEC confirmation subject.
     * <p>
     * It retrieves the subject confirmation of a PEC message with Italian language setting.
     * </p>
     *
     * @return the generated PEC confirmation subject template as a String.
     */
    @Override
    public String generatePecSubjectConfirm() {
        log.debug("retrieve template pecValidationContactsSuccessSubject");
        return templatesClient.pecValidationContactsSuccessSubject(LanguageEnum.IT);
    }

    /**
     * Generates the PEC reject subject.
     * <p>
     * It retrieves the subject reject of a PEC message with Italian language setting.
     * </p>
     *
     * @return the generated PEC reject subject template as a String.
     */
    @Override
    public String generatePecSubjectReject() {
        log.debug("retrieve template pecValidationContactsRejectSubject");
        return templatesClient.pecValidationContactsRejectSubject(LanguageEnum.IT);
    }

    /**
     * Generates the SMS body.
     * <p>
     * It retrieves the SMS body of message with Italian language setting.
     * </p>
     *
     * @return the generated SMS body template as a String.
     */
    @Override
    public String generateSmsBody() {
        log.debug("retrieve template smsVerificationCodeBody");
        return templatesClient.smsVerificationCodeBody(LanguageEnum.IT);
    }
}