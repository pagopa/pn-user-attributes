package it.pagopa.pn.user.attributes.exceptions;

import org.springframework.http.HttpStatus;

public class PnVerificationCodeInvalid extends RuntimeException implements IPnInternalException {
    private static final String VERIFICATION_CODE_MISMATCH = "Verification code mismatch";
    private final PnError pnError;

    public PnVerificationCodeInvalid() {
        this.pnError = new PnError(VERIFICATION_CODE_MISMATCH, HttpStatus.NOT_ACCEPTABLE);
    }

    @Override
    public PnError getPnError() {
        return pnError;
    }
}
