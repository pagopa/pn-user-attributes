package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
public class LegalAddressController implements LegalApi {

    private final AddressBookService addressBookService;
    private final PnAuditLogBuilder auditLogBuilder;

    public LegalAddressController(AddressBookService addressBookService, PnAuditLogBuilder auditLogBuilder) {
        this.addressBookService = addressBookService;
        this.auditLogBuilder = auditLogBuilder;
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

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_DEL, logMessage)
                .uid(recipientId)
                .build();
        logEvent.log();

        return addressBookService.deleteLegalAddressBook(recipientId, senderId, channelType, pnCxType, pnCxGroups, pnCxRole)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
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
        String logMessage = String.format("postRecipientLegalAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_DD_INSUP, logMessage)
                .uid(recipientId)
                .build();
        logEvent.log();

        return addressBookService.saveLegalAddressBook(recipientId, senderId, channelType, addressVerificationDto, pnCxType, pnCxGroups, pnCxRole)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(m -> {
                    log.info("postRecipientLegalAddress done - recipientId={} - senderId={} - channelType={} res={}", recipientId, senderId, channelType, m.toString());
                    if (m == AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED) {
                        return ResponseEntity.ok().build();
                    } else {
                        logEvent.generateSuccess(logMessage).log();
                        return ResponseEntity.noContent().build();
                    }
                });
    }

}
