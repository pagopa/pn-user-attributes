package it.pagopa.pn.user.attributes.middleware.queue.entities;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ActionEvent implements GenericEvent<StandardEventHeader, Action> {

    private StandardEventHeader header;

    private Action payload;
}
