package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.io.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.io.v1.dto.IoCourtesyDigitalAddressActivationDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
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

    public CourtesyIoController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public  Mono<ResponseEntity<IoCourtesyDigitalAddressActivationDto>> getCourtesyAddressIo(String xPagopaPnCxId, final ServerWebExchange exchange) {
        log.info("[enter] getCourtesyAddressIo xPagopaPnCxId={}", xPagopaPnCxId);
        return this.addressBookService.isAppIoEnabledByRecipient(xPagopaPnCxId)
                .map(x -> {
                    IoCourtesyDigitalAddressActivationDto addressActivationDto = new IoCourtesyDigitalAddressActivationDto();
                    addressActivationDto.setActivationStatus(x);
                    log.info("[exit] getCourtesyAddressIo xPagopaPnCxId={}", xPagopaPnCxId);
                    return ResponseEntity.status(HttpStatus.OK).body(addressActivationDto);
                });
    }


    @Override
    public Mono<ResponseEntity<Void>> setCourtesyAddressIo(String xPagopaPnCxId, Mono<IoCourtesyDigitalAddressActivationDto> ioCourtesyDigitalAddressActivationDto, String xPagopaCxTaxid, final ServerWebExchange exchange) {
        String logMessage = String.format("setCourtesyAddressIo - recipientId=%s - senderId=%s - channelType=%s", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO);
        log.info(logMessage);

        return ioCourtesyDigitalAddressActivationDto
                .flatMap(dto -> {

                    if (dto.getActivationStatus())
                    {
                        return this.addressBookService.saveCourtesyAddressBook(xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO,new AddressVerificationDto(), xPagopaCxTaxid)
                                .map(m -> {
                                    log.info("setCourtesyAddressIo done - recipientId={} - senderId={} - channelType={} res={}", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO, m);
                                    return ResponseEntity.noContent().build();
                                });
                    }
                    else
                    {
                        return this.addressBookService.deleteCourtesyAddressBook(xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO)
                                .map(m -> {
                                    log.info("setCourtesyAddressIo done - recipientId={} - senderId={} - channelType={} res={}", xPagopaPnCxId, null, CourtesyChannelTypeDto.APPIO, m);
                                    return ResponseEntity.noContent().build();
                                });
                    }
                });

    }
}
