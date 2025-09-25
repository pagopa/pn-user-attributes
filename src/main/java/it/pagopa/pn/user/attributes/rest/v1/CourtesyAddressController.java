package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.exceptions.PnAddressNotFoundException;
import it.pagopa.pn.user.attributes.exceptions.PnExpiredVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.user.attributes.exceptions.PnRetryLimitVerificationCodeException;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
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

import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;

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

        Optional<PnAuditLogEvent> optionalPnAuditLogEvent;
        if (channelType != CourtesyChannelTypeDto.APPIO) {
            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_AB_DA_DEL, logMessage)
                    .build();
            logEvent.log();
            optionalPnAuditLogEvent = Optional.of(logEvent);
        } else {
             optionalPnAuditLogEvent = Optional.empty();
        }

        return preCheckEmailBeforeDeletion(recipientId, senderId, channelType,pnCxType,pnCxGroups,pnCxRole,optionalPnAuditLogEvent);

    }

    /**
     * Decide se la cancellazione dell'indirizzo di cortesia EMAIL è possibile:
     * - se è presente un indirizzo SERCQ: errore 400, non è possibile la cancellazione
     * - se non è presente un indirizzo SERCQ: si può procedere con la cancellazione
     *
     * Se NON ci troviamo nel channelType EMAIL verranno effettuate le cancellazioni richieste
     */
    private Mono<ResponseEntity<Void>> preCheckEmailBeforeDeletion(String recipientId, String senderId,
                                                                   CourtesyChannelTypeDto channelType,
                                                                   CxTypeAuthFleetDto pnCxType,
                                                                   List<String> pnCxGroups,
                                                                   String pnCxRole,
                                                                   Optional<PnAuditLogEvent> optionalPnAuditLogEvent) {
        log.info("Invoking preCheckEmailBeforeDeletion() for recipientId={} senderId={} channelType={}", recipientId, senderId, channelType);

        if (channelType != CourtesyChannelTypeDto.EMAIL) {
            log.info("ChannelType is not EMAIL, proceeding to delete and skipping preCheck for recipientId={} senderId={}",recipientId,senderId);
            return deleteCourtesyAddress(recipientId, senderId, channelType, pnCxType, pnCxGroups, pnCxRole, optionalPnAuditLogEvent);
        }
        log.info("ChannelType is EMAIL, proceeding with precheck for recipientId={} senderId={} channelType={}", recipientId,senderId,channelType);
        return addressBookService.getLegalAddressByRecipient(recipientId, pnCxType,
                pnCxGroups, pnCxRole)
                .filter(address -> address.getChannelType() == LegalChannelTypeDto.SERCQ)
                .hasElements()
                .flatMap(exists -> {
                    if (exists) {
                        log.error("Deletion blocked: legal address SERCQ is present for recipientId={} senderId={}", recipientId, senderId);
                        return Mono.just(ResponseEntity.badRequest().build());
                    } else {
                        log.info("No SERCQ legal address found, proceeding deleting EMAIL for recipientId={} senderId={} channelType={}", recipientId,senderId,channelType);
                        return deleteCourtesyAddress(recipientId, senderId, channelType, pnCxType, pnCxGroups, pnCxRole, optionalPnAuditLogEvent);
                    }
                });
    }

    private Mono<ResponseEntity<Void>> deleteCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType,
                                                                      CxTypeAuthFleetDto pnCxType, List<String> pnCxGroups, String pnCxRole,
                                                                      Optional<PnAuditLogEvent> optionalPnAuditLogEvent) {
        log.info("Start deleteCourtesyAddress() for recipientId={} senderId={} channelType={}", recipientId, senderId, channelType);
        return addressBookService.deleteCourtesyAddressBook(recipientId, senderId, channelType,
                        pnCxType, pnCxGroups, pnCxRole)
                .onErrorResume(throwable -> {
                    optionalPnAuditLogEvent.ifPresent(logEvent -> logEvent.generateFailure(throwable.getMessage()).log());
                    return Mono.error(throwable);
                })
                .map(m -> {
                    optionalPnAuditLogEvent.ifPresent(logEvent -> logEvent.generateSuccess("Delete executed").log());
                    return ResponseEntity.noContent().build();
                });
    }


    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId,
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
                .flatMap(addressVerificationDtoMdc -> {
                    MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, hashAddress(addressVerificationDtoMdc.getValue()));
                    return MDCUtils.addMDCToContextAndExecute(Mono.just(addressVerificationDtoMdc)
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

                            .flatMap(tupleVerCodeLogEvent -> addressBookService.saveCourtesyAddressBook(recipientId, senderId, channelType, tupleVerCodeLogEvent.getT1(), pnCxType, pnCxGroups, pnCxRole)
                                    .onErrorResume(throwable ->
                                        addressBookService.manageError(tupleVerCodeLogEvent.getT2(),throwable)
                                    )
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
                                    })));
                });
    }

    @NotNull
    private PnAuditLogEvent getLogEvent(String recipientId, CxTypeAuthFleetDto pnCxType, String senderId, CourtesyChannelTypeDto channelType, List<String> pnCxGroups, String pnCxRole) {
        PnAuditLogEvent logEvent;
        String logMessage = String.format("postRecipientCourtesyAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_VALIDATE_CODE, logMessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}
