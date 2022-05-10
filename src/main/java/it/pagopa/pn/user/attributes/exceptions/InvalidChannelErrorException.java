package it.pagopa.pn.user.attributes.exceptions;

public class InvalidChannelErrorException extends PnException {


    public InvalidChannelErrorException() {
        super("Canale non valido", "Canale non attualmente supportato");
    }

}
