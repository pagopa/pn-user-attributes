package it.pagopa.pn.user.attributes.middleware.templates;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;

public interface TemplateGenerator {

    /**
     * Generates the email body.
     *
     * @param verificationCode the code to include in the email body for verification purposes.
     * @return the generated email body template as a String.
     */
    String generateEmailBody(String verificationCode, String recipientType, LanguageEnum language);

    /**
     * Generates the email subject.
     *
     * @return the generated email subject template as a String.
     */
    String generateEmailSubject(LanguageEnum language);

    /**
     * Generates the PEC body.
     *
     * @param verificationCode the code to include in the PEC body for verification purposes.
     * @param recipientType the recipient type.
     * @return the generated PEC body template as a String.
     */
    String generatePecBody(String verificationCode, String recipientType, LanguageEnum language);

    /**
     * Generates the PEC confirmation body.
     *
     * @return the generated PEC confirmation body template as a String.
     */
    String generatePecConfirmBody(String recipientType, LanguageEnum language);

    /**
     * Generates the PEC reject body.
     *
     * @return the generated PEC reject body template as a String.
     */
    String generatePecRejectBody(String recipientType, LanguageEnum language);

    /**
     * Generates the PEC subject.
     *
     * @return the generated PEC subject template as a String.
     */
    String generatePecSubject(LanguageEnum language);

    /**
     * Generates the PEC confirmation subject.
     *
     * @return the generated PEC confirmation subject template as a String.
     */
    String generatePecSubjectConfirm(LanguageEnum language);

    /**
     * Generates the PEC reject subject.
     *
     * @return the generated PEC reject subject template as a String.
     */
    String generatePecSubjectReject(LanguageEnum language);

    /**
     * Generates the SMS body.
     *
     * @return the generated SMS body template as a String.
     */
    String generateSmsBody(LanguageEnum language);
}