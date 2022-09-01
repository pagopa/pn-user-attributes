package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_ADDRESS_NOT_FOUND;

public class PnAddressNotFoundException extends PnRuntimeException {

    public PnAddressNotFoundException() {
        super("Indirizzo non presente", "Non è stato trovato un codice verifica valido", HttpStatus.NOT_FOUND.value(), ERROR_CODE_USERATTRIBUTES_ADDRESS_NOT_FOUND, null, null);
    }

}