package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/address-book-private/v1/digital-address")
public class AddressPrivateControllerWrapper {
    private final LegalAddressController legalAddress;
    private final CourtesyAddressController courtesyAddressController;

    /**
     * GET /address-book-private/v1/digital-address/legal/{recipientId}/{senderId}
     */
    @GetMapping("/legal/{recipientId}/{senderId}")
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySender(
            @PathVariable("recipientId") @Pattern(regexp = "^[ -~ ]*$") @Size(max = 39) String recipientId,
            @PathVariable("senderId") @Pattern(regexp = "^[ -~ ]*$") @Size(max = 50) String senderId,
            ServerWebExchange exchange) {
        return legalAddress.getLegalAddressBySender(recipientId, senderId, exchange);
    }

    /**
     *  GET /address-book-private/v1/digital-address/courtesy/{recipientId}/{senderId}
     */
    @GetMapping("/courtesy/{recipientId}/{senderId}")
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySender(
            @PathVariable("recipientId") @Pattern(regexp = "^[ -~ ]*$") @Size(max = 39) String recipientId,
            @PathVariable("senderId") @Pattern(regexp = "^[ -~ ]*$") @Size(max = 50) String senderId,
            ServerWebExchange exchange) {
        return courtesyAddressController.getCourtesyAddressBySender(recipientId, senderId, exchange);
    }

}
