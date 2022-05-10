package it.pagopa.pn.user.attributes.exceptions;

public class InternalErrorException extends PnException {


    public InternalErrorException() {
        super("Errore interno", "Errore applicativo interno", 500);
    }

}
