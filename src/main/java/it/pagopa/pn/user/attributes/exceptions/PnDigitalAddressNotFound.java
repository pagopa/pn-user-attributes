package it.pagopa.pn.user.attributes.exceptions;

import org.springframework.http.HttpStatus;

public class PnDigitalAddressNotFound extends RuntimeException implements IPnInternalException {
    private static final String ERROR_ADDRESS_NOT_FOUND = "Digital Address not found";
    private final PnError pnError;

    public PnDigitalAddressNotFound() {
        this.pnError = new PnError(ERROR_ADDRESS_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Override
    public PnError getPnError() {
        return pnError;
    }
}
