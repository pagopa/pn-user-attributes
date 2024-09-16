package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto.TOS_DEST_B2B;

@Slf4j
public class ConsentsUtils {
    public static final Set<String> ALLOWED_CONSENT_TYPE = Set.of(TOS_DEST_B2B.getValue());

    public static Mono<Void> validatePgConsentAction(String consentType, String cxRole, List<String> groups) {
        log.debug("validatePgConsentAction - consentType={} - cxRole={} - groups={}", consentType, cxRole, groups);
        if (!ALLOWED_CONSENT_TYPE.contains(consentType)) {
            return Mono.error(new PnForbiddenException());
        }

        if(!isRoleAdmin(cxRole, groups)) {
            return Mono.error(new PnForbiddenException());
        }
        return Mono.empty();
    }

    public static boolean isRoleAdmin(String xRole, List<String> groups) {
        return "ADMIN".equals(xRole) && (groups == null || groups.isEmpty());
    }

    public static Mono<Void> validateCxType(CxTypeAuthFleetDto xPagopaPnCxType) {
        log.debug("validateCxType - xPagopaPnCxType={}", xPagopaPnCxType);
        if (!Objects.equals(CxTypeAuthFleetDto.PG.toString(), xPagopaPnCxType.getValue())) {
            return Mono.error(new PnForbiddenException());
        }
        return Mono.empty();
    }

}