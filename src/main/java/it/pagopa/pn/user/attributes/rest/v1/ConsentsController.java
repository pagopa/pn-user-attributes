package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.api.ConsentsApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
public class ConsentsController implements ConsentsApi {
    private final ConsentsService consentsService;

    public ConsentsController(ConsentsService consentsService) {
        this.consentsService = consentsService;
    }

    @Override
    public Mono<ResponseEntity<Void>> consentAction(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType,
                                                    String version, Mono<ConsentActionDto> consentActionDto,  final ServerWebExchange exchange) {
        String logMessage = String.format("consentAction - xPagopaPnUid=%s - xPagopaPnCxType=%s - consentType=%s - version=%s", xPagopaPnUid, xPagopaPnCxType, consentType, version);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_UC_INSUP, logMessage)
                .build();
        logEvent.log();
        return consentActionDto.flatMap(dto -> {
                    String messageAction = String.format("xPagopaPnUid=%s - xPagopaPnCxType=%s - consentType=%s - version=%s - consentAction=%s", xPagopaPnUid, xPagopaPnCxType, consentType, version, dto.getAction().toString());
                    return this.consentsService.consentAction(xPagopaPnUid, xPagopaPnCxType,  consentType, dto, version)
                            .onErrorResume(throwable -> {
                                logEvent.generateFailure(throwable.getMessage()).log();
                                return Mono.error(throwable);
                            })
                            .then(Mono.fromRunnable(() -> logEvent.generateSuccess(messageAction).log()));
                })
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    @Override
    public Mono<ResponseEntity<ConsentDto>> getConsentByType(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, String version,  final ServerWebExchange exchange) {
        log.info("getConsentByType - xPagopaPnUid={} - xPagopaPnCxType={} - consentType={} - version={}", xPagopaPnUid, xPagopaPnCxType, consentType, version);

        return this.consentsService.getConsentByType(xPagopaPnUid, xPagopaPnCxType, consentType, version)
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Flux<ConsentDto>>> getConsents(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType,  final ServerWebExchange exchange) {
        log.info("getConsents - xPagopaPnUid={} - xPagopaPnCxType={}", xPagopaPnUid, xPagopaPnCxType);

        return Mono.fromSupplier(() -> ResponseEntity.ok(this.consentsService.getConsents(xPagopaPnUid, xPagopaPnCxType)));
    }

    /**
     * PUT /pg-consents/v1/consents/{consentType} : Accept a single consent type
     * Accept single consent type for the recipient
     *
     * @param xPagopaPnCxId Customer/Receiver Identifier (required)
     * @param xPagopaPnCxType Customer/Receiver Type (required)
     * @param consentType A cosa sto dando il consenso (required)
     * @param xPagopaPnCxRole User role (required)
     * @param version La versione del consenso, obbligatoria in fase di accettazione. (required)
     * @param consentActionDto  (required)
     * @param xPagopaPnCxGroups Customer Groups (optional)
     * @return successful operation (status code 200)
     *         or Invalid input (status code 400)
     *         or Forbidden (status code 403)
     */
    @Override
    public Mono<ResponseEntity<Void>> setPgConsentAction(String xPagopaPnCxId, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, String xPagopaPnCxRole, String version, Mono<ConsentActionDto> consentActionDto, List<String> xPagopaPnCxGroups, final ServerWebExchange exchange) {
        String logMessage = String.format("pgConsentAction - xPagopaPnCxId=%s - xPagopaPnCxType=%s - consentType=%s - version=%s", xPagopaPnCxId, xPagopaPnCxType, consentType, version);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_UC_INSUP, logMessage)
                .build();
        logEvent.log();
        return consentActionDto.flatMap(dto -> {
                    String messageAction = String.format("xPagopaPnCxId=%s - xPagopaPnCxType=%s - consentType=%s - version=%s - consentAction=%s", xPagopaPnCxId, xPagopaPnCxType, consentType, version, dto);
                    return this.consentsService.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, dto, xPagopaPnCxGroups)
                            .onErrorResume(throwable -> {
                                logEvent.generateFailure(throwable.getMessage()).log();
                                return Mono.error(throwable);
                            })
                            .then(Mono.fromRunnable(() -> logEvent.generateSuccess(messageAction).log()));
                })
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }


    /**
     * GET /pg-consents/v1/consents/{consentType} : Get single consent by type
     * Returns single consent type for the recipient. Return a Consent with accepted false if consent type is not found.
     *
     * @param xPagopaPnCxId Customer/Receiver Identifier (required)
     * @param xPagopaPnCxType Customer/Receiver Type (required)
     * @param consentType A cosa sto dando il consenso (required)
     * @param version La versione del consenso. se non presente il default è nessuna versione accettata. (optional)
     * @return successful operation (status code 200)
     *         or Invalid input (status code 400)
     *         or Forbidden (status code 403)
     */
    @Override
    public Mono<ResponseEntity<ConsentDto>> getPgConsentByType(String xPagopaPnCxId, CxTypeAuthFleetDto xPagopaPnCxType,
                                                               ConsentTypeDto consentType, String version, final ServerWebExchange exchange) {
        log.info("getPgConsentByType - xPagopaPnCxId={} - xPagopaPnCxType={} - consentType={} - version={}",
                xPagopaPnCxId, xPagopaPnCxType, consentType, version);
        return this.consentsService.getPgConsentByType(xPagopaPnCxId, xPagopaPnCxType, consentType, version)
                .map(ResponseEntity::ok);
    }
}

