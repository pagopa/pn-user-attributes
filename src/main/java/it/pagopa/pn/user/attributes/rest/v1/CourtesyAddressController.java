package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.exceptions.PnAddressNotFoundException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.*;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
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
public class CourtesyAddressController implements CourtesyApi {

    private final AddressBookService addressBookService;

    public CourtesyAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientCourtesyAddress(String recipientId,
                                                                     CxTypeAuthFleetDto pnCxType,
                                                                     String senderId,
                                                                     CourtesyChannelTypeDto channelType,
                                                                     List<String> pnCxGroups,
                                                                     String pnCxRole,
                                                                     ServerWebExchange exchange) {
        String logMessage = String.format("deleteRecipientCourtesyAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(channelType == CourtesyChannelTypeDto.APPIO ? PnAuditLogEventType.AUD_AB_DA_IO_DEL : PnAuditLogEventType.AUD_AB_DA_DEL, logMessage)
                .build();
        logEvent.log();

        return addressBookService.deleteCourtesyAddressBook(recipientId, senderId, channelType, pnCxType, pnCxGroups, pnCxRole)
                .onErrorResume(throwable -> {
                    if (throwable instanceof PnAddressNotFoundException) {
                        logEvent.generateWarning(throwable.getMessage()).log();
                    } else {
                        logEvent.generateFailure(throwable.getMessage()).log();
                    }
                    return Mono.error(throwable);
                })
                .map(m -> {
                    logEvent.generateSuccess(logMessage).log();
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyAndUnverifiedDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId,
                                                                                               CxTypeAuthFleetDto pnCxType,
                                                                                               List<String> pnCxGroups,
                                                                                               String pnCxRole,
                                                                                               ServerWebExchange exchange) {
        log.info("getCourtesyAddressByRecipient - recipientId={} - cxType={} - cxRole={} - cxGroups={}", recipientId, pnCxType, pnCxRole, pnCxGroups);
        return Mono.fromSupplier(() -> ResponseEntity.ok(addressBookService.getCourtesyAddressByRecipient(recipientId, pnCxType, pnCxGroups, pnCxRole)));
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.info("getCourtesyAddressBySender - recipientId={} - senderId={}", recipientId, senderId);
        return Mono.fromSupplier(() ->  ResponseEntity.ok(addressBookService.getCourtesyAddressByRecipientAndSender(recipientId, senderId)));
    }

    @Override
    public Mono<ResponseEntity<AddressVerificationResponseDto>> postRecipientCourtesyAddress(String recipientId,
                                                                   CxTypeAuthFleetDto pnCxType,
                                                                   String senderId,
                                                                   CourtesyChannelTypeDto channelType,
                                                                   Mono<AddressVerificationDto> addressVerificationDto,
                                                                   List<String> pnCxGroups,
                                                                   String pnCxRole,
                                                                   ServerWebExchange exchange) {


        return addressVerificationDto
                .map(addressVerificationDto1 -> {
                    // l'auditLog va creato solo se sto creando effettivamente (quindi o è APPIO oppure è una richiesta con codice di conferma)
                    Optional<PnAuditLogEvent> auditLogEvent;
                    if (StringUtils.hasText(addressVerificationDto1.getVerificationCode())
                            || channelType == CourtesyChannelTypeDto.APPIO) {
                        auditLogEvent = Optional.of(getLogEvent(recipientId, pnCxType, senderId, channelType, pnCxGroups, pnCxRole));
                    }
                    else {
                        auditLogEvent = Optional.empty();
                    }

                    return Tuples.of(addressVerificationDto1, auditLogEvent);
                })
                .flatMap(tupleVerCodeLogEvent -> addressBookService.saveCourtesyAddressBook(recipientId, senderId, channelType, tupleVerCodeLogEvent.getT1(), pnCxType, pnCxGroups, pnCxRole)
                        .onErrorResume(throwable -> {
                            if (throwable instanceof PnInvalidVerificationCodeException)
                                tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateWarning("codice non valido - {}",throwable.getMessage()).log());
                            else
                                tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateFailure(throwable.getMessage()).log());
                            return Mono.error(throwable);
                        })
                        .map(m -> {
                            log.info("postRecipientCourtesyAddress done - recipientId={} - senderId={} - channelType={} res={}", recipientId, senderId, channelType, m.toString());
                            if (m != AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS) {
                                AddressVerificationResponseDto responseDto = new AddressVerificationResponseDto();
                                responseDto.result(AddressVerificationResponseDto.ResultEnum.fromValue(m.toString()));
                                return ResponseEntity.ok(responseDto);
                            } else {
                                tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateSuccess().log());
                                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                            }
                        }));
    }

    @NotNull
    private PnAuditLogEvent getLogEvent(String recipientId, CxTypeAuthFleetDto pnCxType, String senderId, CourtesyChannelTypeDto channelType, List<String> pnCxGroups, String pnCxRole) {
        PnAuditLogEvent logEvent;
        String logMessage = String.format("postRecipientCourtesyAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        logEvent = auditLogBuilder
                .before(channelType == CourtesyChannelTypeDto.APPIO ? PnAuditLogEventType.AUD_AB_DA_IO_INSUP : PnAuditLogEventType.AUD_AB_DA_INSUP, logMessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}
