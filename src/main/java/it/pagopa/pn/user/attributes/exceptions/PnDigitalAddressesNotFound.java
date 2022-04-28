package it.pagopa.pn.user.attributes.exceptions;

import org.springframework.http.HttpStatus;

public class PnDigitalAddressesNotFound extends RuntimeException implements IPnInternalException {
    private static final String ERROR_ADDRESSES_NOT_FOUND = "Digital Addresses not found";
    private final PnError pnError;

    public PnDigitalAddressesNotFound() {
        this.pnError = new PnError(ERROR_ADDRESSES_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Override
    public PnError getPnError() {
        return pnError;
    }
}
