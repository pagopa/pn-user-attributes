package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.utils.LanguageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static it.pagopa.pn.user.attributes.handler.PecRequestPrefixes.PEC_REJECTED_PREFIX;

@Slf4j
@AllArgsConstructor
@Component
public class PecValidationExpiredResponseHandler {

    private final PnExternalChannelClient externalChannelClient;


    public Mono<Void> consumePecValidationExpiredEvent(String internalId, String address, String language) {
        return externalChannelClient.sendCourtesyPecRejected(PEC_REJECTED_PREFIX + UUID.randomUUID(), internalId, address, LanguageUtils.resolveLanguage(language))
                .then();
    }

}
