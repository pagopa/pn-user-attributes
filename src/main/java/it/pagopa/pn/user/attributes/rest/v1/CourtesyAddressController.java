package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.v1.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class CourtesyAddressController implements CourtesyApi {

    AddressBookService addressBookService;

    public CourtesyAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, ServerWebExchange exchange) {
        log.debug("deleteRecipientCourtesyAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);

        return this.addressBookService.deleteAddressBook(recipientId, senderId, false, channelType.getValue())
                .map(m -> ResponseEntity.status(HttpStatus.NO_CONTENT).body(null));
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        log.debug("getCourtesyAddressByRecipient - recipientId: {}", recipientId);

        return this.addressBookService.getCourtesyAddressByRecipient(recipientId).collectList().map(dtos -> {
            if (dtos.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            else
                return ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(dtos));
        });

    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.debug("getCourtesyAddressBySender - recipientId: {} - senderId: {}", recipientId, senderId);
        return this.addressBookService.getCourtesyAddressBySender(recipientId, senderId).collectList().map(dtos -> {
            if (dtos.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            else
                return ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(dtos));
        });
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        log.debug("postRecipientCourtesyAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);

        return this.addressBookService.saveAddressBook(recipientId, senderId, false, channelType.getValue(), addressVerificationDto)
                .map(m -> {
                    log.info("fine addressBookService.saveAddressBookEx - {}", m.booleanValue());
                    if (m.booleanValue())
                        return ResponseEntity.status(HttpStatus.OK).body(null);
                    else
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                });
    }

}
