package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.user.attributes.middleware.db.BaseDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.junit.jupiter.api.Assertions.*;

class PnInvalidInputExceptionTest {

    private String code = "405";
    private String field = "test-";


    @Test
    void pnInvalidInputExceptionConstructorTest() {
        PnInvalidInputException pnInvalidInputException = new PnInvalidInputException(code, field);

        assertEquals(pnInvalidInputException.getProblem().getErrors().get(0).getCode(),code);
        assertEquals(pnInvalidInputException.getProblem().getErrors().get(0).getElement(),field);
    }

}