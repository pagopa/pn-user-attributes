package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
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
public class CourtesyAddressController implements CourtesyApi {
    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, ServerWebExchange exchange) {
        return CourtesyApi.super.deleteRecipientCourtesyAddress(recipientId, senderId, channelType, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        return CourtesyApi.super.getCourtesyAddressByRecipient(recipientId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        return CourtesyApi.super.getCourtesyAddressBySender(recipientId, senderId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        return CourtesyApi.super.postRecipientCourtesyAddress(recipientId, senderId, channelType, addressVerificationDto, exchange);
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String>  handleException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

}
