package it.pagopa.pn.user.attributes.middleware.queue.entities;

import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.SendMessageRequest;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Action {

    private String actionId;

    private String internalId;

    private Instant lastDisabledStateTransitionTimestamp;

    private SendMessageRequest messageRequest;

    private Instant timestamp;

    private ActionType type;
}
