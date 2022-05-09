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
        return Mono.fromSupplier(() -> ResponseEntity.ok(this.addressBookService.getLegalAddressByRecipient(recipientId)));
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        log.debug("getLegalAddressBySender - recipientId: {} - senderId: {}", recipientId, senderId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(this.addressBookService.getLegalAddressByRecipientAndSender(recipientId, senderId)));

    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        log.info("postRecipientLegalAddress - recipientId: {} - senderId: {} - channelType: {}", recipientId, senderId, channelType);
        return this.addressBookService.saveAddressBook(recipientId, senderId, true, channelType.getValue(), addressVerificationDto)
                .map(m -> {
                    log.debug("postRecipientLegalAddress done - recipientId: {} - senderId: {} - channelType: {} res: {}", recipientId, senderId, channelType, m.toString());
                    if (m == AddressBookService.SAVE_ADDRESS_RESULT.CODE_VERIFICATION_REQUIRED)
                        return ResponseEntity.ok().build();
                    else
                        return ResponseEntity.noContent().build();
                });

    }

}
