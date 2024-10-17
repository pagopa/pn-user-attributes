package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.exceptions.PnNotFoundException;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@lombok.CustomLog
@NoArgsConstructor(access = AccessLevel.NONE)
public class PgUtils {

    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN");

    /**
     * Effettua la validazione dell'accesso per le Persone Giuridiche.
     *
     * @param pnCxType   tipo utente (PF, PG, PA)
     * @param pnCxRole   ruolo (admin, operator)
     * @param pnCxGroups gruppi
     */
    public static Mono<Object> validaAccesso(CxTypeAuthFleetDto pnCxType, String pnCxRole, List<String> pnCxGroups) {
        String process = "validating access admin only";
        log.logChecking(process);
        if (CxTypeAuthFleetDto.PG.equals(pnCxType)
                && (pnCxRole == null || !ALLOWED_ROLES.contains(pnCxRole.toUpperCase()) || !CollectionUtils.isEmpty(pnCxGroups))) {
            log.logCheckingOutcome(process, false, "only a PG admin can access this resource");
            return Mono.error(new PnNotFoundException());
        }
        log.debug("access granted for {}, role: {}, groups: {}", pnCxType, pnCxRole, pnCxGroups);
        log.logCheckingOutcome(process, true);
        return Mono.just(new Object());
    }

}
