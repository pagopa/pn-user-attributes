package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_RETRYLIMIT_VERIFICATION_CODE;

public class PnRetryLimitVerificationCodeException extends PnRuntimeException {

    public PnRetryLimitVerificationCodeException() {
        super("Codice verifica non valido. Massimo numero di tentativi raggiunto", "Il codice passato non è corretto. Massimo numero di tentativi raggiunto", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_RETRYLIMIT_VERIFICATION_CODE, null, null);
    }

}
