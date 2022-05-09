package it.pagopa.pn.user.attributes.exceptions;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ProblemDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@Slf4j
public class ExceptionHelper {

    public static final String MDC_TRACE_ID_KEY = "trace_id";

    private ExceptionHelper(){}

    public static HttpStatus getHttpStatusFromException(Throwable ex){
        if (ex instanceof PnException)
        {
            return HttpStatus.resolve(((PnException) ex).getStatus());
        }
        else
            return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public static ProblemDto handleException(Throwable ex, HttpStatus statusError){
        // gestione exception e generazione fault
        ProblemDto res = new ProblemDto();
        res.setStatus(statusError.value());
        try {
            res.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        } catch (Exception e) {
            log.warn("Cannot get traceid", e);
        }

        if (ex instanceof PnException)
        {
            res.setTitle(ex.getMessage());
            res.setDetail(((PnException)ex).getDescription());
            res.setStatus(((PnException) ex).getStatus());
        }
        else
        {
            // nascondo all'utente l'errore
            res.title("Errore generico");
            res.detail("Qualcosa è andato storto, ritenta più tardi");
        }

        return res;
    }
}
