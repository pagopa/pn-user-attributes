package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN;

public class SercqDisabledException extends PnRuntimeException {
    public SercqDisabledException(String message) {
        super("Sercq is not enabled", message, HttpStatus.BAD_REQUEST.value(), ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, null, null);
    }
}
