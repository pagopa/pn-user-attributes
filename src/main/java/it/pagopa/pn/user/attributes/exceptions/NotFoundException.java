package it.pagopa.pn.user.attributes.exceptions;

public class NotFoundException extends PnException {


    public NotFoundException() {
        super("Indirizzo non trovato", "Il codice passato non è corretto", 404);
    }

}
