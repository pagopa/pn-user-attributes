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
    private final ConsentsService consentsService;
    private final PnAuditLogBuilder auditLogBuilder;

    public ConsentsController(ConsentsService consentsService, PnAuditLogBuilder pnAuditLogBuilder) {
        this.consentsService = consentsService;
        this.auditLogBuilder = pnAuditLogBuilder;
    }

    @Override
    public Mono<ResponseEntity<Void>> consentAction(String recipientId, ConsentTypeDto consentType, Mono<ConsentActionDto> consentActionDto, String version, ServerWebExchange exchange) {
        String logMessage = String.format("consentAction - recipientId=%s - consentType=%s - version=%s", recipientId, consentType, version);

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_UC_INSUP, logMessage)
                .uid(recipientId)
                .build();
        logEvent.log();
        return consentActionDto.flatMap(dto -> {
                    String messageAction = String.format("recipientId=%s - consentType=%s - version=%s - consentAction=%s", recipientId, consentType, version, dto.getAction().toString());
                    return this.consentsService.consentAction(recipientId, consentType, dto, version)
                            .onErrorResume(throwable -> {
                                logEvent.generateFailure(throwable.getMessage()).log();
                                return Mono.error(throwable);
                            })
                            .then(Mono.fromRunnable(() -> logEvent.generateSuccess(messageAction).log()));
                })
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String recipientId, ConsentTypeDto consentType, String version, ServerWebExchange exchange) {
        log.info("getConsentByType - recipientId={} - consentType={} - version={}", recipientId, consentType, version);

        return this.consentsService.getConsentByType(recipientId, consentType, version)
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String recipientId, ServerWebExchange exchange) {
        log.info("getConsents - recipientId={} ", recipientId);

        return Mono.fromSupplier(() -> ResponseEntity.ok(this.consentsService.getConsents(recipientId)));
    }
}

