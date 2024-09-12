package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ConsentsUtils {
    public static Mono<Void> validateCxType(CxTypeAuthFleetDto xPagopaPnCxType) {
        if (!Objects.equals(CxTypeAuthFleetDto.PG.toString(), xPagopaPnCxType.getValue())) {
            return Mono.error(new PnForbiddenException());
        }
        return Mono.empty();
    }

}
