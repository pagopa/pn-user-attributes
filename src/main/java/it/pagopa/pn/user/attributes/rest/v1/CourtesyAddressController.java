package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.CourtesyApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.services.v1.ConsentsService;
import it.pagopa.pn.user.attributes.services.v1.CourtesyVerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
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

    CourtesyVerificationCodeService verifcationCodeService;

    public CourtesyAddressController(CourtesyVerificationCodeService verifcationCodeService) {
        this.verifcationCodeService = verifcationCodeService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, ServerWebExchange exchange) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
        return result.then(Mono.empty());
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(String recipientId, ServerWebExchange exchange) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return result.then(Mono.empty());
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(String recipientId, String senderId, ServerWebExchange exchange) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return result.then(Mono.empty());
    }

    @Override
    public Mono<ResponseEntity<Void>> postRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto, ServerWebExchange exchange) {
        return this.verifcationCodeService.postRecipientCourtesyAddress(recipientId, senderId, channelType, addressVerificationDto)
                .map(m -> ResponseEntity.status(HttpStatus.OK).body(null));

//        Mono<Void> result = Mono.empty();
//        exchange.getResponse().setStatusCode(HttpStatus.CREATED);
//        return result.then(Mono.empty());
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

    // catch IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String>  handleException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

}
