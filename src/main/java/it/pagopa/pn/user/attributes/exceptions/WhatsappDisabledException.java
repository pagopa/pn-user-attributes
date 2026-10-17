package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN;

/**
 * Sollevata quando si tenta di creare un recapito di cortesia WHATSAPP con la funzionalità disattivata
 * (pn.user-attributes.isWhatsappEnabled=false).
 */
public class WhatsappDisabledException extends PnRuntimeException {
    public WhatsappDisabledException(String message) {
        super("WhatsApp is not enabled", message, HttpStatus.BAD_REQUEST.value(), ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, null, null);
    }
}
