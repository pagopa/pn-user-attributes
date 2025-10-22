package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.user.attributes.exceptions.PnExceptionInsertingAddress;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.Problem;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.ProblemError;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static it.pagopa.pn.user.attributes.services.utils.ConstantsError.ERROR_ACTIVATION_LEGAL;
import static it.pagopa.pn.user.attributes.services.utils.ConstantsError.ERROR_TITLE_LEGAL;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/address-book/v1/digital-address/legal")
public class LegalAddressControllerWrapper {

    private final LegalAddressController legalAddress;

    /**
     * POST /address-book/v1/digital-address/legal/{senderId}/{channelType}
     */
    @PostMapping("/{senderId}/{channelType}")
    public Mono<ResponseEntity<Object>> postRecipientLegalAddress(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @PathVariable String senderId,
            @PathVariable LegalChannelTypeDto channelType,
            @RequestBody Mono<AddressVerificationDto> addressVerificationDto,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {

        return legalAddress.postRecipientLegalAddress(
                        recipientId, pnCxType, senderId, channelType,
                        addressVerificationDto, pnCxGroups, pnCxRole, exchange)
                .flatMap(resp -> Mono.justOrEmpty(resp.getBody())
                        .map(body -> ResponseEntity.status(resp.getStatusCode())
                                .headers(resp.getHeaders())
                                .body((Object) body))
                        .defaultIfEmpty(ResponseEntity.noContent().build()))
                .onErrorResume(PnExceptionInsertingAddress.class, e -> {
                    Problem problem = new Problem()
                            .title(ERROR_TITLE_LEGAL)
                            .detail(ERROR_ACTIVATION_LEGAL)
                            .traceId(MDC.get(MDC_TRACE_ID_KEY))
                            .status(HttpStatus.BAD_REQUEST.value())
                            .errors(List.of(new ProblemError()
                                    .code(ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR)
                                    .detail(e.getMessage())));
                    return Mono.just(
                            ResponseEntity.badRequest()
                                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                    .body(problem)
                    );
                });
    }

    /**
     * GET /address-book-private/v1/digital-address/legal/{recipientId}/{senderId}
     */
    @GetMapping
    public Mono<ResponseEntity<Object>> getLegalAddressByRecipient(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {

        return legalAddress.getLegalAddressByRecipient(recipientId, pnCxType, pnCxGroups, pnCxRole, exchange)
                .map(resp -> ResponseEntity.status(resp.getStatusCode())
                        .headers(resp.getHeaders())
                        .body(resp.getBody()));
    }

    /**
     * DELETE /address-book/v1/digital-address/legal/{senderId}/{channelType}
     */
    @DeleteMapping("/{senderId}/{channelType}")
    public Mono<ResponseEntity<Void>> deleteRecipientLegalAddress(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @PathVariable String senderId,
            @PathVariable LegalChannelTypeDto channelType,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {
        return legalAddress.deleteRecipientLegalAddress(recipientId, pnCxType, senderId, channelType, pnCxGroups, pnCxRole, exchange);
    }


}
