package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.ConsentsApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
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
    public Mono<ResponseEntity<Void>> consentAction(String recipientId, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, Mono<ConsentActionDto> consentActionDto, String version, final ServerWebExchange exchange) {
        String logMessage = String.format("consentAction - xPagopaPnUid=%s - xPagopaPnCxType=%s - consentType=%s - version=%s", recipientId, xPagopaPnCxType, consentType, version);

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_UC_INSUP, logMessage)
                .build();
        logEvent.log();
        return consentActionDto.flatMap(dto -> {
                    String messageAction = String.format("xPagopaPnUid=%s - xPagopaPnCxType=%s - consentType=%s - version=%s - consentAction=%s", recipientId, xPagopaPnCxType, consentType, version, dto.getAction().toString());
                    return this.consentsService.consentAction(recipientId, xPagopaPnCxType,  consentType, dto, version)
                            .onErrorResume(throwable -> {
                                logEvent.generateFailure(throwable.getMessage()).log();
                                return Mono.error(throwable);
                            })
                            .then(Mono.fromRunnable(() -> logEvent.generateSuccess(messageAction).log()));
                })
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String recipientId, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, String version,  final ServerWebExchange exchange) {
        log.info("getConsentByType - xPagopaPnUid={} - xPagopaPnCxType={} - consentType={} - version={}", recipientId, xPagopaPnCxType, consentType, version);

        return this.consentsService.getConsentByType(recipientId, xPagopaPnCxType, consentType, version)
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String recipientId, CxTypeAuthFleetDto xPagopaPnCxType,  final ServerWebExchange exchange) {
        log.info("getConsents - recipientId={} - xPagopaPnCxType={}", recipientId, xPagopaPnCxType);

        return Mono.fromSupplier(() -> ResponseEntity.ok(this.consentsService.getConsents(recipientId, xPagopaPnCxType)));
    }
}

