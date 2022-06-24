package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
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
        String logMessage = String.format("consentAction - recipientId=%s - consentType=%s", recipientId, consentType);
        log.info(logMessage);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_UC_INSUP, logMessage)
                .build();
        return consentActionDto.flatMap(dto -> {
                String messageAction = String.format("consentAction=%s", dto.getAction().toString());
                return this.consentsService.consentAction(recipientId, consentType, dto)
                        .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable)
                            .then(Mono.just(logEvent.generateSuccess(messageAction).log()));
                });
            })
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String recipientId, ConsentTypeDto consentType, ServerWebExchange exchange) {
        log.info("getConsentByType - recipientId={} - consentType={}", recipientId, consentType);

        return this.consentsService.getConsentByType(recipientId, consentType)
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String recipientId, ServerWebExchange exchange) {
        log.info("getConsents - recipientId={} ", recipientId);

        return Mono.fromSupplier(() -> ResponseEntity.ok(this.consentsService.getConsents(recipientId)));
    }
}

