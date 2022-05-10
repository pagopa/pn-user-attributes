package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.ConsentsApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ConsentsController implements ConsentsApi {
    ConsentsService consentsService;

    public ConsentsController(ConsentsService consentsService) {
        this.consentsService = consentsService;
    }

    @Override
    public Mono<ResponseEntity<Void>> consentAction(String recipientId, ConsentTypeDto consentType, Mono<ConsentActionDto> consentActionDto, ServerWebExchange exchange) {
        log.debug("consentAction - recipientId: {} - consentType: {}", recipientId, consentType);

        return this.consentsService.consentAction(recipientId, consentType, consentActionDto)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String recipientId, ConsentTypeDto consentType, ServerWebExchange exchange) {
        log.debug("getConsentByType - recipientId: {} - consentType: {}", recipientId, consentType);

        return this.consentsService.getConsentByType(recipientId, consentType)
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String recipientId, ServerWebExchange exchange) {
        log.debug("getConsents - recipientId: {} ", recipientId);

        return Mono.fromSupplier(() -> ResponseEntity.ok(this.consentsService.getConsents(recipientId)));
    }
}

