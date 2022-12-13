package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

class PgUtilsTest {

    @Test
    void testValidaAccessoPF() {
        StepVerifier.create(PgUtils.validaAccesso(CxTypeAuthFleetDto.PF, null, null))
                .expectNextCount(1)
                .expectComplete();
        StepVerifier.create(PgUtils.validaAccesso(null, null, null))
                .expectNextCount(1)
                .expectComplete();
    }

    @Test
    void testValidaAccessoPG() {
        StepVerifier.create(PgUtils.validaAccesso(CxTypeAuthFleetDto.PG, "admin", null))
                .expectNextCount(1)
                .expectComplete();
        StepVerifier.create(PgUtils.validaAccesso(CxTypeAuthFleetDto.PG, "admin", Collections.emptyList()))
                .expectNextCount(1)
                .expectComplete();
    }

    @Test
    void testValidaAccessoPG_Failure() {
        StepVerifier.create(PgUtils.validaAccesso(CxTypeAuthFleetDto.PG, "operator", null))
                .expectError(PnForbiddenException.class)
                .verify();
        StepVerifier.create(PgUtils.validaAccesso(CxTypeAuthFleetDto.PG, "admin", List.of("")))
                .expectError(PnForbiddenException.class)
                .verify();
    }

}