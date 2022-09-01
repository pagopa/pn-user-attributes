package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_INVALID_VERIFICATION_CODE;

public class PnInvalidVerificationCodeException extends PnRuntimeException {

    public PnInvalidVerificationCodeException() {
        super("Codice verifica non valido", "Il codice passato non è corretto", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_INVALID_VERIFICATION_CODE, null, null);
    }

}
