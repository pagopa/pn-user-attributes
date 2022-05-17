package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
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
        log.info("deleteRecipientCourtesyAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);

        return this.addressBookService.deleteCourtesyAddressBook(recipientId, senderId, channelType)
                .map(m -> ResponseEntity.status(HttpStatus.NO_CONTENT).body(null));
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        log.info("getCourtesyAddressByRecipient - recipientId: {}", recipientId);
        return Mono.fromSupplier(() ->  ResponseEntity.ok(this.addressBookService.getCourtesyAddressByRecipient(recipientId)));
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.info("getCourtesyAddressBySender - recipientId: {} - senderId: {}", recipientId, senderId);
        return Mono.fromSupplier(() ->  ResponseEntity.ok(this.addressBookService.getCourtesyAddressByRecipientAndSender(recipientId, senderId)));
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        log.info("postRecipientCourtesyAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);

        return this.addressBookService.saveCourtesyAddressBook(recipientId, senderId, channelType, addressVerificationDto)
                .map(m -> {
                    log.info("postRecipientCourtesyAddress done - recipientId: {} - senderId: {} - channelType: {} res: {}", recipientId, senderId, channelType, m.toString());
                    if (m == AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED)
                        return ResponseEntity.status(HttpStatus.OK).body(null);
                    else
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                });
    }

}
