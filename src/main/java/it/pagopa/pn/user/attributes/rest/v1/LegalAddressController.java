package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.LegalApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;

@RestController
@Slf4j
public class LegalAddressController implements LegalApi {
    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, ServerWebExchange exchange) {
        return LegalApi.super.deleteRecipientLegalAddress(recipientId, senderId, channelType, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        return LegalApi.super.getLegalAddressByRecipient(recipientId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        return LegalApi.super.getLegalAddressBySender(recipientId, senderId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientLegalAddress(String recipientId, String senderId, LegalChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        return LegalApi.super.postRecipientLegalAddress(recipientId, senderId, channelType, addressVerificationDto, exchange);
    }

    // catch ConstraintViolationException
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<String> handleValidationException(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // catch TypeMismatchException
    @ExceptionHandler({TypeMismatchException.class})
    public ResponseEntity<String> handleIllegalArgumentException(TypeMismatchException ex) {

        return ResponseEntity.badRequest().body(ex.getMessage());
    }

}
