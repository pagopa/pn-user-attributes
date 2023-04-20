package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_PEC_ADDRESS;

public class PnInvalidPecException extends PnRuntimeException {

    public PnInvalidPecException() {
        super("L'indirizzo usato non è stato validato come PEC", "L'indirizzo usato non è una PEC valida", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_INVALID_PEC_ADDRESS, null, null);
    }

}
