package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.services.AddressBookService;
import it.pagopa.pn.user.attributes.user.attributes.b2b.generated.openapi.server.v1.api.CourtesyB2BApi;
import it.pagopa.pn.user.attributes.user.attributes.b2b.generated.openapi.server.v1.api.LegalB2BApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
public class B2bAddressController implements LegalB2BApi, CourtesyB2BApi{
    private final AddressBookService addressBookService;

    public B2bAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalDigitalAddressDto>>> getLegalAddressBySenderB2B(String xPagopaPnCxId, CxTypeAuthFleetDto xPagopaPnCxType, String senderId, List<String> xPagopaPnCxGroups, String xPagopaPnCxRole, final ServerWebExchange exchange) {
        log.info("getLegalAddressBySender - recipientId={} - senderId={}", xPagopaPnCxId, senderId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(addressBookService.getLegalAddressByRecipientAndSender(xPagopaPnCxId, senderId)));
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressBySenderB2B(String xPagopaPnCxId, CxTypeAuthFleetDto xPagopaPnCxType, String senderId, List<String> xPagopaPnCxGroups, String xPagopaPnCxRole, final ServerWebExchange exchange) {
        log.info("getCourtesyAddressBySender - recipientId={} - senderId={}", xPagopaPnCxId, senderId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(addressBookService.getCourtesyAddressByRecipientAndSender(xPagopaPnCxId, senderId)));
    }

}
