package it.pagopa.pn.user.attributes.exceptions;

public class InvalidVerificationCodeException extends PnException {


    public InvalidVerificationCodeException() {
        super("Codice verifica non valido", "Il codice passato non è corretto", 406);
    }

}
