package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.user.attributes.exceptions.PnExceptionDeletingAddress;
import it.pagopa.pn.user.attributes.exceptions.PnExceptionInsertingAddress;
import it.pagopa.pn.user.attributes.services.utils.ConstantsError;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
public class HandleErrorController {

    @ExceptionHandler(PnExceptionInsertingAddress.class)
    public final ResponseEntity<Problem> handleInsertingLegalAddressException(PnExceptionInsertingAddress pnExceptionInsertingAddress){
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle(ConstantsError.ERROR_TITLE_LEGAL);
        problem.setDetail(ConstantsError.ERROR_ACTIVATION_LEGAL);
        problem.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        problem.setErrors(List.of(new ProblemError()
                .code(ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR)
                .detail(pnExceptionInsertingAddress.getMessage())));
        return ResponseEntity
                .status(BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(problem);
    }

    @ExceptionHandler(PnExceptionDeletingAddress.class)
    public final ResponseEntity<Problem> handleDeletingCourtesyAddressException(PnExceptionDeletingAddress pnExceptionDeletingAddress){
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle(ConstantsError.ERROR_TITLE_COURTESY);
        problem.setDetail(ConstantsError.ERROR_DELETE_COURTESY);
        problem.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        problem.setErrors(List.of(new ProblemError()
                .code(ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR)
                .detail(pnExceptionDeletingAddress.getMessage())));
        return ResponseEntity
                .status(BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(problem);
    }

}
