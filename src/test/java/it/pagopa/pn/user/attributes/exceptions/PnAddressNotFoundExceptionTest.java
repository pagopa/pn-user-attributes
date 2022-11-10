package it.pagopa.pn.user.attributes.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PnAddressNotFoundExceptionTest {


    @Test
    void pnInvalidInputExceptionConstructorTest() {
        PnAddressNotFoundException pnAddressNotFoundException = new PnAddressNotFoundException();
        assertNotNull(pnAddressNotFoundException);
        assertNotNull(pnAddressNotFoundException.getMessage());
    }

}