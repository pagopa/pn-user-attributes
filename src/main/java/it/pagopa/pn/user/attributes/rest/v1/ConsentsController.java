package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.api.ConsentsApi;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;

@RestController
@Slf4j
public class ConsentsController implements ConsentsApi {
    @Override
    public Mono<ResponseEntity<Void>> consentAction(String recipientId, ConsentTypeDto consentType, Mono<ConsentActionDto> consentActionDto, ServerWebExchange exchange) {
        return ConsentsApi.super.consentAction(recipientId, consentType, consentActionDto, exchange);
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String recipientId, ConsentTypeDto consentType, ServerWebExchange exchange) {
        return ConsentsApi.super.getConsentByType(recipientId, consentType, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String recipientId, ServerWebExchange exchange) {
        return ConsentsApi.super.getConsents(recipientId, exchange);
    }

    // catch ConstraintViolationException
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<String> handleValidationException(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // catch TypeMismatchException
    @ExceptionHandler({TypeMismatchException.class})
    public ResponseEntity<String> handleIllegalArgumentException(TypeMismatchException ex) {

        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
