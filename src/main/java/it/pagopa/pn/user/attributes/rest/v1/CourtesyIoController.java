package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.io.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.io.api.v1.dto.IoCourtesyDigitalAddressActivationDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class CourtesyIoController implements CourtesyApi {

    private final AddressBookService addressBookService;
    private final PnAuditLogBuilder auditLogBuilder;

    public CourtesyIoController(AddressBookService addressBookService, PnAuditLogBuilder auditLogBuilder) {
        this.addressBookService = addressBookService;
        this.auditLogBuilder = auditLogBuilder;
    }

    @Override
    public  Mono<ResponseEntity<IoCourtesyDigitalAddressActivationDto>> getCourtesyAddressIo(String xPagopaPnCxId, final ServerWebExchange exchange) {
        return this.addressBookService.isAppIoEnabledByRecipient(xPagopaPnCxId)
                .map(x -> {
                    IoCourtesyDigitalAddressActivationDto addressActivationDto = new IoCourtesyDigitalAddressActivationDto();
                    addressActivationDto.setActivationStatus(x);
                    return ResponseEntity.status(HttpStatus.OK).body(addressActivationDto);
                });
    }


    @Override
    public  Mono<ResponseEntity<Void>> setCourtesyAddressIo(String xPagopaPnCxId, Mono<IoCourtesyDigitalAddressActivationDto> ioCourtesyDigitalAddressActivationDto,  final ServerWebExchange exchange) {
        String logMessage = String.format("setCourtesyAddressIo - recipientId=%s - senderId=%s - channelType=%s", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO);
        log.info(logMessage);

        return ioCourtesyDigitalAddressActivationDto
                .flatMap(dto -> {

                    if (dto.getActivationStatus())
                    {
                        PnAuditLogEvent logEvent = auditLogBuilder
                                .before(PnAuditLogEventType.AUD_AB_DA_IO_INSUP, logMessage)
                                .build();
                        logEvent.log();
                        return this.addressBookService.saveCourtesyAddressBook(xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO, Mono.just(new AddressVerificationDto()))
                                .onErrorResume(throwable -> {
                                    logEvent.generateFailure(throwable.getMessage()).log();
                                    return Mono.error(throwable);
                                })
                                .map(m -> {
                                    log.info("setCourtesyAddressIo done - recipientId={} - senderId={} - channelType={} res={}", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO, m.toString());
                                    logEvent.generateSuccess(logMessage).log();
                                    return ResponseEntity.noContent().build();
                                });
                    }
                    else
                    {
                        PnAuditLogEvent logEvent = auditLogBuilder
                                .before(PnAuditLogEventType.AUD_AB_DA_IO_DEL, logMessage)
                                .build();
                        logEvent.log();
                        return this.addressBookService.deleteCourtesyAddressBook(xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO)
                                .onErrorResume(throwable -> {
                                    logEvent.generateFailure(throwable.getMessage()).log();
                                    return Mono.error(throwable);
                                })
                                .map(m -> {
                                    log.info("setCourtesyAddressIo done - recipientId={} - senderId={} - channelType={} res={}", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO, m.toString());
                                    logEvent.generateSuccess(logMessage).log();
                                    return ResponseEntity.noContent().build();
                                });
                    }
                });

    }
}
