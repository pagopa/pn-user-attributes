package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.api.AllApi;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.UserAddressesDto;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
public class AllAddressController implements AllApi {

    private final AddressBookService addressBookService;

    public AllAddressController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    @Override
    public Mono<ResponseEntity<UserAddressesDto>> getAddressesByRecipient(String recipientId,
                                                                          CxTypeAuthFleetDto pnCxType,
                                                                          List<String> pnCxGroups,
                                                                          String pnCxRole,
                                                                          ServerWebExchange exchange) {
        log.info("getAddressesByRecipient - recipientId: {} - type: {} - groups: {} - role: {}", recipientId, pnCxType, pnCxGroups, pnCxRole);
        return addressBookService.getAddressesByRecipient(recipientId, pnCxType, pnCxGroups, pnCxRole)
                .map(userAddressesDto -> ResponseEntity.status(HttpStatus.OK).body(userAddressesDto));
    }
}
