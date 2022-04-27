package it.pagopa.pn.user.attributes.exceptions;

import org.springframework.http.HttpStatus;

public class PnInternalException extends RuntimeException implements IPnInternalException {
    private final PnError pnError;

    public PnInternalException(String message, HttpStatus status) {
        this.pnError = new PnError(message, status);
    }

    public PnInternalException(PnError pnError) {
        this.pnError = pnError;
    }

    @Override
    public PnError getPnError() {
        return pnError;
    }
}
