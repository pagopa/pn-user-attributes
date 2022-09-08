package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnInvalidInputException extends PnValidationException {

    public PnInvalidInputException(String errorCode, String field) {
        super("Input non valido", List.of(ProblemError.builder()
                .code(errorCode)
                .element(field)
                .build()), null );
    }

}
