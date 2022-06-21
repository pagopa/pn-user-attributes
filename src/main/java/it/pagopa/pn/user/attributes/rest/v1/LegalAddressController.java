package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;

@RestController
@Slf4j
public class LegalAddressController implements LegalApi {

    AddressBookService addressBookService;

    public LegalAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, ServerWebExchange exchange) {
        String logMessage = String.format("deleteRecipientLegalAddress - recipientId: %s - senderId: %s - channelType: %s", recipientId, senderId, channelType);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_DEL, logMessage)
                .build();
        return this.addressBookService.deleteLegalAddressBook(recipientId, senderId, channelType)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                }).map(m -> {
                    logEvent.generateSuccess().log();
                    return ResponseEntity.noContent().build();
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        log.info("getLegalAddressByRecipient - recipientId: {}", recipientId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(this.addressBookService.getLegalAddressByRecipient(recipientId)));
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.info("getLegalAddressBySender - recipientId: {} - senderId: {}", recipientId, senderId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(this.addressBookService.getLegalAddressByRecipientAndSender(recipientId, senderId)));

    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        String logMessage = String.format("postRecipientLegalAddress - recipientId: %s - senderId: %s - channelType: %s", recipientId, senderId, channelType);
        log.info(logMessage);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_INSUP, logMessage)
                .build();
        return this.addressBookService.saveLegalAddressBook(recipientId, senderId, channelType, addressVerificationDto)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(m -> {
                    log.info("postRecipientLegalAddress done - recipientId: {} - senderId: {} - channelType: {} res: {}", recipientId, senderId, channelType, m.toString());
                    if (m == AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED)
                        return ResponseEntity.ok().build();
                    else {
                        logEvent.generateSuccess().log();
                        return ResponseEntity.noContent().build();
                    }
                });

    }

}
