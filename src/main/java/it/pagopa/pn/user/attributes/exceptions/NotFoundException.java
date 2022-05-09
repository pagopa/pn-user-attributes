package it.pagopa.pn.user.attributes.exceptions;

public class NotFoundException extends PnException {


    public NotFoundException() {
        super("Recapito non trovato", "Non sono stati trovati recapiti per l'utente", 404);
    }

}
