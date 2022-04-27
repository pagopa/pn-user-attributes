package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
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
public class LegalAddressController implements LegalApi {

    AddressBookService addressBookService;

    public LegalAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, ServerWebExchange exchange) {
        log.debug("deleteRecipientLegalAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);
        return this.addressBookService.deleteAddressBook(recipientId, senderId, true, channelType.getValue())
                .map(m -> ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        log.debug("getLegalAddressByRecipient - recipientId: {}", recipientId);
        return this.addressBookService.getLegalAddressByRecipient(recipientId).collectList().map(dtos -> {
            if (dtos.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            else
                return ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(dtos));
        });
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.debug("getLegalAddressBySender - recipientId: {} - senderId: {}", recipientId, senderId);
        return this.addressBookService.getLegalAddressBySender(recipientId, senderId).collectList().map(dtos -> {
            if (dtos.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            else
                return ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(dtos));
        });
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        log.debug("postRecipientLegalAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);
        return this.addressBookService.saveAddressBook(recipientId, senderId, true, channelType.getValue(), addressVerificationDto)
                .map(m -> {
                    log.info("fine addressBookService.saveAddressBookEx - {}", m.booleanValue());
                    if (m.booleanValue())
                        return ResponseEntity.status(HttpStatus.OK).body(null);
                    else
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                });

    }

}
