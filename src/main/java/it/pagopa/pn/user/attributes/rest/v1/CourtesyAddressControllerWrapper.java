package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.user.attributes.exceptions.PnExceptionDeletingAddress;
import it.pagopa.pn.user.attributes.services.utils.ConstantsError;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.Problem;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.ProblemError;
import java.util.List;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR;
import static it.pagopa.pn.user.attributes.services.utils.ConstantsError.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/address-book/v1/digital-address/courtesy")
public class CourtesyAddressControllerWrapper {
    private final CourtesyAddressController courtesyAddressController;


    /**
     * DELETE /address-book/v1/digital-address/courtesy/{senderId}/{channelType}
     */
    @DeleteMapping("/{senderId}/{channelType}")
    public Mono<ResponseEntity<Object>> deleteRecipientCourtesyAddress(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @PathVariable String senderId,
            @PathVariable CourtesyChannelTypeDto channelType,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {

        return courtesyAddressController.deleteRecipientCourtesyAddress(
                        recipientId, pnCxType, senderId, channelType, pnCxGroups, pnCxRole, exchange)
                .map(resp -> ResponseEntity.noContent().build())
                .onErrorResume(PnExceptionDeletingAddress.class, e -> {
                    Problem problem = new Problem()
                            .detail(ERROR_DELETE_COURTESY)
                            .traceId(MDC.get(MDCUtils.MDC_TRACE_ID_KEY))
                            .title(ERROR_TITLE_COURTESY)
                            .status(HttpStatus.BAD_REQUEST.value())
                            .errors(List.of(new ProblemError()
                                    .code(ERROR_CODE_PN_HTTPRESPONSE_GENERIC_ERROR)
                                    .detail(e.getMessage())));
                    return Mono.just(ResponseEntity
                            .badRequest()
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .body(problem));
                });
    }

    /**
     * GET /address-book/v1/digital-address/courtesy
     */
    @GetMapping
    public Mono<ResponseEntity<Flux<CourtesyDigitalAddressDto>>> getCourtesyAddressByRecipient(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {

        return courtesyAddressController.getCourtesyAddressByRecipient(recipientId, pnCxType, pnCxGroups, pnCxRole, exchange);
    }


    /**
     * POST /address-book/v1/digital-address/courtesy/{senderId}/{channelType}
     */
    @PostMapping("/{senderId}/{channelType}")
    public Mono<ResponseEntity<Object>> postRecipientCourtesyAddress(
            @RequestHeader("x-pagopa-pn-cx-id") String recipientId,
            @RequestHeader("x-pagopa-pn-cx-type") CxTypeAuthFleetDto pnCxType,
            @PathVariable String senderId,
            @PathVariable CourtesyChannelTypeDto channelType,
            @RequestBody Mono<AddressVerificationDto> addressVerificationDto,
            @RequestHeader(value = "x-pagopa-pn-cx-groups", required = false) List<String> pnCxGroups,
            @RequestHeader(value = "x-pagopa-pn-cx-role", required = false) String pnCxRole,
            ServerWebExchange exchange) {

        return courtesyAddressController.postRecipientCourtesyAddress(
                        recipientId, pnCxType, senderId, channelType, addressVerificationDto, pnCxGroups, pnCxRole, exchange)
                .map(resp -> {
                    if (resp.getBody() == null) {
                        return ResponseEntity.noContent().build();
                    } else {
                        return ResponseEntity.status(resp.getStatusCode())
                                .headers(resp.getHeaders())
                                .body(resp.getBody());
                    }
                });
    }

}
