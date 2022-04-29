package it.pagopa.pn.user.attributes.exceptions;

import org.springframework.http.HttpStatus;

public class PnDigitalAddressDeletionFailure extends RuntimeException implements IPnInternalException {
    private static final String ERROR_DIGITAL_ADDRESS_DELETION_FAILURE = "Digital Address deletion failure";
    private final PnError pnError;

    public PnDigitalAddressDeletionFailure() {
        this.pnError = new PnError(ERROR_DIGITAL_ADDRESS_DELETION_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public PnError getPnError() {
        return pnError;
    }
}
