package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.exceptions.PnAddressNotFoundException;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.services.ConsentsService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.user.attributes.utils.HashingUtils.hashAddress;

@RestController
@Slf4j
public class LegalAddressController implements LegalApi {

    private final AddressBookService addressBookService;
    private final ConsentsService consentsService;

    public LegalAddressController(AddressBookService addressBookService, ConsentsService consentsService) {
        this.addressBookService = addressBookService;
        this.consentsService = consentsService;
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
    public Mono<ResponseEntity<Flux<LegalAndUnverifiedDigitalAddressDto>>> getLegalAddressByRecipient(String recipientId,
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
    public Mono<ResponseEntity<AddressVerificationResponseDto>> postRecipientLegalAddress(String recipientId,
                                                                                          CxTypeAuthFleetDto pnCxType,
                                                                                          String senderId,
                                                                                          LegalChannelTypeDto channelType,
                                                                                          Mono<AddressVerificationDto> addressVerificationDto,
                                                                                          List<String> pnCxGroups,
                                                                                          String pnCxRole,
                                                                                          ServerWebExchange exchange) {

        return addressVerificationDto
                .flatMap(addressVerificationDtoMdc -> {
                    MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, hashAddress(addressVerificationDtoMdc.getValue()));

                    // Recupero della lista di indirizzi tramite recipientId e senderId
                    Flux<LegalDigitalAddressDto> addressesList = addressBookService.getLegalAddressByRecipientAndSender(recipientId, senderId);

                    // Filtro gli indirizzi in base al tipo di canale
                    Flux<LegalDigitalAddressDto> filteredAddresses = addressesList
                            .filter(address -> channelType == LegalChannelTypeDto.SERCQ ? address.getChannelType() == LegalChannelTypeDto.PEC
                                    : address.getChannelType() == LegalChannelTypeDto.SERCQ);

                    // Recupero dei consensi
                    Flux<ConsentDto> consentsFlux = consentsService.getConsents(recipientId, pnCxType);

                    return consentsFlux
                            .collectList()
                            .flatMap(consentsList -> {
                                boolean isSercq = channelType == LegalChannelTypeDto.SERCQ;

                               Boolean hasConsents = checkConsents(recipientId, consentsList, isSercq);
                                if (Boolean.FALSE.equals(hasConsents)) return Mono.just(ResponseEntity.badRequest().body(new AddressVerificationResponseDto()));

                                return filteredAddresses.collectList()
                                        .flatMap(filteredAddressesList ->
                                                Flux.fromIterable(filteredAddressesList)
                                                        .flatMap(address ->
                                                                addressBookService.deleteLegalAddressBook(address.getRecipientId(), address.getSenderId(), address.getChannelType(), pnCxType, pnCxGroups, pnCxRole, true)
                                                                        .cast(TransactDeleteItemEnhancedRequest.class)
                                                                        .map(deleteResponse -> deleteResponse) // risposta del dao
                                                                        .onErrorResume(e -> {
                                                                            log.error("Error deleting address: {}", address, e);
                                                                            return Mono.empty();
                                                                        })
                                                        )
                                                        .collectList()
                                        )
                                        .flatMap(deleteResponses ->
                                                executePostLegalAddressLogic(recipientId, pnCxType, senderId, channelType,
                                                        addressVerificationDtoMdc, pnCxGroups, pnCxRole, deleteResponses.isEmpty() ? null : deleteResponses));
                            });
                })
                .onErrorResume(e -> {
                    // Gestione degli errori
                    log.error("Error occurred while processing address verification", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }


    private static Boolean checkConsents(String recipientId, List<ConsentDto> consentsList, boolean isSercq) {
        if (isSercq) {
            boolean hasTosConsent = consentsList.stream()
                    .anyMatch(consent -> ConsentTypeDto.TOS_SERCQ.equals(consent.getConsentType()) && Boolean.TRUE.equals(consent.getAccepted()));

            boolean hasPrivacyConsent = consentsList.stream()
                    .anyMatch(consent -> ConsentTypeDto.DATAPRIVACY_SERCQ.equals(consent.getConsentType()) && Boolean.TRUE.equals(consent.getAccepted()));

            if (!(hasTosConsent && hasPrivacyConsent)) {
                log.warn("Consents TOS and PRIVACY are missing for recipientId: {}", recipientId);
                return false;
            }
        }
        return true;
    }

    private Mono<ResponseEntity<AddressVerificationResponseDto>> executePostLegalAddressLogic(String recipientId,
                                                                                              CxTypeAuthFleetDto pnCxType,
                                                                                              String senderId,
                                                                                              LegalChannelTypeDto channelType,
                                                                                              AddressVerificationDto addressVerificationDtoMdc,
                                                                                              List<String> pnCxGroups,
                                                                                              String pnCxRole, List<TransactDeleteItemEnhancedRequest> deleteItemResponses) {

        return MDCUtils.addMDCToContextAndExecute(Mono.just(addressVerificationDtoMdc)
                .map(addressVerificationDto1 -> {
                    // l'auditLog va creato solo se sto creando effettivamente (quindi o è APPIO oppure è una richiesta con codice di conferma)
                    Optional<PnAuditLogEvent> auditLogEvent;
                    if (StringUtils.hasText(addressVerificationDto1.getVerificationCode())) {
                        auditLogEvent = Optional.of(getLogEvent(recipientId, pnCxType, senderId, channelType, pnCxGroups, pnCxRole));
                    } else {
                        auditLogEvent = Optional.empty();
                    }

                    return Tuples.of(addressVerificationDto1, auditLogEvent);
                })
                .flatMap(tupleVerCodeLogEvent -> addressBookService.saveLegalAddressBook(recipientId, senderId, channelType,
                                tupleVerCodeLogEvent.getT1(), pnCxType, pnCxGroups, pnCxRole, deleteItemResponses)
                                .onErrorResume(throwable ->
                                        addressBookService.manageError(tupleVerCodeLogEvent.getT2(),throwable)
                                )

                        .map(m -> {
                            log.info("postRecipientLegalAddress done - recipientId={} - senderId={} - channelType={} res={}",
                                    recipientId, senderId, channelType, m.toString());

                            if (m != AddressBookService.SAVE_ADDRESS_RESULT.SUCCESS) {
                                AddressVerificationResponseDto responseDto = new AddressVerificationResponseDto();
                                responseDto.result(AddressVerificationResponseDto.ResultEnum.fromValue(m.toString()));
                                return ResponseEntity.ok(responseDto);
                            } else {
                                tupleVerCodeLogEvent.getT2().ifPresent(pnAuditLogEvent -> pnAuditLogEvent.generateSuccess().log());
                                return ResponseEntity.noContent().build();
                            }
                        })));
    }




    @NotNull
    private PnAuditLogEvent getLogEvent(String recipientId, CxTypeAuthFleetDto pnCxType, String senderId, LegalChannelTypeDto channelType, List<String> pnCxGroups, String pnCxRole) {
        String logMessage = String.format("postRecipientLegalAddress - recipientId=%s - senderId=%s - channelType=%s - cxType=%s - cxRole=%s - cxGroups=%s",
                recipientId, senderId, channelType, pnCxType, pnCxRole, pnCxGroups);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_VALIDATE_CODE, logMessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}
