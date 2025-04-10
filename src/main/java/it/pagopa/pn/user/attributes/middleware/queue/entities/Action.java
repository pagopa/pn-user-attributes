package it.pagopa.pn.user.attributes.middleware.queue.entities;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV25;
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

    private Instant checkFromWhen;

    private SentNotificationV25 sentNotification;

    private Instant timestamp;

    private ActionType type;

    @lombok.ToString.Exclude private String address;
}
