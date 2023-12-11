package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Component
public class PecValidationExpiredResponseHandler {

    private final PnExternalChannelClient externalChannelClient;

    private static final String PEC_INVALID_PREFIX = "pec-rejected-";


    public Mono<Void> consumePecValidationExpiredEvent(String internalId, String address) {
       return  externalChannelClient.sendCourtesyPecRejected(PEC_INVALID_PREFIX + UUID.randomUUID(), internalId, address)
               .then();

    }

}
