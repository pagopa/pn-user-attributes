package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.api.AllApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.UserAddressesDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class AllAddressController implements AllApi {
    @Override
    public Mono<ResponseEntity<UserAddressesDto>> getAddressesByRecipient(String recipientId, ServerWebExchange exchange) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return result.then(Mono.empty());
    }
}

