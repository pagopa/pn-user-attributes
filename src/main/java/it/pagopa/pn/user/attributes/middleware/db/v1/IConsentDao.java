package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IConsentDao {
    Mono<Object> consentAction(ConsentEntity userAttributes);
    Mono<ConsentEntity> getConsentByType(String recipientId, String consentType);
    Flux<ConsentEntity> getConsents(String recipientId);
}
