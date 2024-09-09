package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ConsentsUtils {
    public static final Set<String> ALLOWED_CONTENT_TYPE = Set.of("TOS_DEST_B2B");

    public static Mono<Void> validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPE.contains(contentType)) {
            return Mono.error(new PnForbiddenException());
        }
        return Mono.empty();
    }

    public static Mono<Void> validateCxType(CxTypeAuthFleetDto xPagopaPnCxType) {
        if (!Objects.equals(CxTypeAuthFleetDto.PG.toString(), xPagopaPnCxType.getValue())) {
            return Mono.error(new PnForbiddenException());
        }
        return Mono.empty();
    }

    public static Mono<Boolean> isRoleAdmin(String xRole, List<String> groups) {
        return Mono.just("ADMIN".equals(xRole) && (groups == null || groups.isEmpty()));
    }
}