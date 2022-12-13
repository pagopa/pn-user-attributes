package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTIBUTES_FORBIDDEN;

public class PnForbiddenException extends PnRuntimeException {

    public PnForbiddenException() {
        super("Accesso negato!", "L'utente non è autorizzato ad accedere alla risorsa richiesta.",
                HttpStatus.FORBIDDEN.value(), ERROR_CODE_USERATTIBUTES_FORBIDDEN, null, null);
    }

}
