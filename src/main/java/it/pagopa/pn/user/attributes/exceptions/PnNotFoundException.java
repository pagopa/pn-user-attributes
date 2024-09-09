package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_ADDRESS_NOT_FOUND;

public class PnNotFoundException extends PnRuntimeException {

    public PnNotFoundException() {
        super("Accesso negato!", "L'utente non è autorizzato ad accedere alla risorsa richiesta.",
                HttpStatus.NOT_FOUND.value(), ERROR_CODE_USERATTRIBUTES_ADDRESS_NOT_FOUND, null, null);
    }
}
