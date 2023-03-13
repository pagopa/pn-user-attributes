package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.exceptions.PnAddressNotFoundException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
public class LegalAddressController implements LegalApi {

    private final AddressBookService addressBookService;

    public LegalAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientLegalAddress(String recipientId,
                                                                  CxTypeAuthFleetDto pnCxType,
                                                                  String senderId,
                                                                  LegalChannelTypeDto channelType,
                                                                  List<String> pnCxGroups,
                                                                  String pnCxRole,
                                                                  ServerWebExchange exchange) {
        String logMessage = String.format("deleteRecipientLegalAddress - recipientId: %s - senderId: %s - channelType: %s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_DEL, logMessage)
                .build();
        logEvent.log();

        return addressBookService.deleteLegalAddressBook(recipientId, senderId, channelType, pnCxType, pnCxGroups, pnCxRole)
                .onErrorResume(throwable -> {
                    if (throwable instanceof PnAddressNotFoundException) {
                        logEvent.generateWarning(throwable.getMessage()).log();
                    } else {
                        logEvent.generateFailure(throwable.getMessage()).log();
                    }
                    return Mono.error(throwable);
                }).map(m -> {
                    logEvent.generateSuccess(logMessage).log();
                    return ResponseEntity.noContent().build();
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressByRecipient(String recipientId,
                                                                                         CxTypeAuthFleetDto pnCxType,
                                                                                         List<String> pnCxGroups,
                                                                                         String pnCxRole,
                                                                                         ServerWebExchange exchange) {
        log.info("getLegalAddressByRecipient - recipientId={} - cxType={} - cxRole={} - cxGroups={}", recipientId, pnCxType, pnCxRole, pnCxGroups);
        return Mono.fromSupplier(() -> ResponseEntity.ok(addressBookService.getLegalAddressByRecipient(recipientId, pnCxType, pnCxGroups, pnCxRole)));
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.info("getLegalAddressBySender - recipientId={} - senderId={}", recipientId, senderId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(addressBookService.getLegalAddressByRecipientAndSender(recipientId, senderId)));
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientLegalAddress(String recipientId,
                                                                CxTypeAuthFleetDto pnCxType,
                                                                String senderId,
                                                                LegalChannelTypeDto channelType,
                                                                Mono<AddressVerificationDto> addressVerificationDto,
                                                                List<String> pnCxGroups,
                                                                String pnCxRole,
                                                                ServerWebExchange exchange) {

        return addressVerificationDto
                .map(addressVerificationDto1 -> {
                    // l'auditLog va creato solo se sto creando effettivamente (quindi o è APPIO oppure è una richiesta con codice di conferma)
                    Optional<PnAuditLogEvent> auditLogEvent;
                    if (StringUtils.hasText(addressVerificationDto1.getVerificationCode())) {
                        auditLogEvent = Optional.of(getLogEvent(recipientId, pnCxType, senderId, channelType, pnCxGroups, pnCxRole));
                    }
                    else {
                        auditLogEvent = Optional.empty();
                    }

                    return Tuples.of(addressVerificationDto1, auditLogEvent);
                })
                .flatMap(tupleVerCodeLogEvent ->  addressBookService.saveLegalAddressBook(recipientId, senderId, channelType, tupleVerCodeLogEvent.getT1(), pnCxType, pnCxGroups, pnCxRole)
                                    .onErrorResume(throwable -> {
                                        if (throwable instanceof PnInvalidVerificationCodeException)
                                            tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateSuccess("FAILURE {}",throwable.getMessage()).log());
                                        else
                                            tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateFailure(throwable.getMessage()).log());
                                        return Mono.error(throwable);
                                    })
                                    .map(m -> {
                                        log.info("postRecipientLegalAddress done - recipientId={} - senderId={} - channelType={} res={}", recipientId, senderId, channelType, m.toString());
                                        if (m == AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED) {
                                            return ResponseEntity.ok().build();
                                        } else {
                                            tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateSuccess().log());
                                            return ResponseEntity.noContent().build();
                                        }
                                    }));
    }


    @NotNull
    private PnAuditLogEvent getLogEvent(String recipientId, CxTypeAuthFleetDto pnCxType, String senderId, LegalChannelTypeDto channelType, List<String> pnCxGroups, String pnCxRole) {
        String logMessage = String.format("postRecipientLegalAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_INSUP, logMessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}
