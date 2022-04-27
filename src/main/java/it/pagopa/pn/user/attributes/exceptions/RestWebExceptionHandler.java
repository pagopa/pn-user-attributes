package it.pagopa.pn.user.attributes.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Order(-2)
@Slf4j
public
class RestWebExceptionHandler implements WebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Throwable rootCause = findExceptionRootCause(ex);
        ObjectMapper oMapper = new ObjectMapper();

        if (rootCause instanceof PnInternalException) {
            String jsonStr = null;
            PnError error = ((PnInternalException) ex).getPnError();
            exchange.getResponse().setStatusCode(error.getHttpStatus());
            return Mono.empty();
        } else if (rootCause instanceof ConstraintViolationException ||
                   rootCause instanceof TypeMismatchException ||
                   rootCause instanceof IllegalArgumentException ||
                   rootCause instanceof WebExchangeBindException) {
//            String jsonStr = null;
//            PnError error = new PnError( GENERIC_BAD_REQUEST_MESSAGE, HttpStatus.BAD_REQUEST);
//
//            try {
//                jsonStr = oMapper.writeValueAsString(error);
//            }
//            catch (IOException e) {
//                return Mono.error(ex);
//            }
//            exchange.getResponse().setStatusCode(error.getHttpStatus());
//            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
//
//            byte[] bytes = jsonStr.getBytes(StandardCharsets.UTF_8);
//            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
//            return exchange.getResponse().writeWith(Flux.just(buffer));

            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return Mono.empty();
        } else if (rootCause instanceof  PnDigitalAddressNotFound) {
            PnDigitalAddressNotFound exc = (PnDigitalAddressNotFound)rootCause;
            log.warn(exc.getPnError().getMessage());
            exchange.getResponse().setStatusCode(exc.getPnError().getHttpStatus());
            return Mono.empty();
        } else if (rootCause instanceof  PnVerificationCodeInvalid) {
            PnVerificationCodeInvalid exc = (PnVerificationCodeInvalid)rootCause;
            log.warn(exc.getPnError().getMessage());
            exchange.getResponse().setStatusCode(exc.getPnError().getHttpStatus());
            return Mono.empty();
        }

        log.warn("Exception", ex);
        return Mono.error(ex);
    }

    public static Throwable findExceptionRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}

