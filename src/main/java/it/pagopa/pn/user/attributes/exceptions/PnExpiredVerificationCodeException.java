package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_EXPIRED_VERIFICATION_CODE;

public class PnExpiredVerificationCodeException extends PnRuntimeException {

    public PnExpiredVerificationCodeException() {
        super("Codice verifica non trovato", "Il codice passato non è presente", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_EXPIRED_VERIFICATION_CODE, null, null);
    }

}
