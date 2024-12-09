package it.pagopa.pn.user.attributes.middleware.legalfacts;

public interface TemplateGenerator {

    String generateEmailBody(String verificationCode);

    String generateEmailSubject();

    String generatePecBody(String verificationCode);

    String generatePecConfirmBody();

    String generatePecRejectBody();

    String generatePecSubject();

    String generatePecSubjectConfirm();

    String generatePecSubjectReject();

    String generateSmsBody();

}
