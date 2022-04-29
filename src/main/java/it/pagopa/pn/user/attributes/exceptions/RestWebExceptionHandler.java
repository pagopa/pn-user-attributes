package it.pagopa.pn.user.attributes.exceptions;

import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import javax.validation.ConstraintViolationException;
import java.util.Objects;

@Component
@Order(-2)
@Slf4j
public
class RestWebExceptionHandler implements WebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Throwable rootCause = findExceptionRootCause(ex);

        if (rootCause instanceof ConnectTimeoutException) {
            log.error("ConnectTimeoutException: {}", ex.getMessage());
            log.error("Check DynamoDB configuration params");
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        } else if (rootCause instanceof ResourceNotFoundException) {
            log.error("ResourceNotFoundException: {}", ex.getMessage());
            log.error("Check DynamoDB table UserAttributes");
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        } else if (rootCause instanceof ConstraintViolationException ||
            rootCause instanceof TypeMismatchException ||
            rootCause instanceof IllegalArgumentException ||
            rootCause instanceof WebExchangeBindException) {

            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            log.warn("Exception (Bad Request): {}", ex.getMessage());
            return Mono.empty();
        } else if (rootCause instanceof  PnDigitalAddressNotFound) {
            PnDigitalAddressNotFound exc = (PnDigitalAddressNotFound) rootCause;
            log.warn(exc.getPnError().getMessage());
            exchange.getResponse().setStatusCode(exc.getPnError().getHttpStatus());
            return Mono.empty();
        } else if (rootCause instanceof  PnDigitalAddressesNotFound) {
            PnDigitalAddressesNotFound exc = (PnDigitalAddressesNotFound)rootCause;
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

