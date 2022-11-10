package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDeliveryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ActionHandlerTest {

    private ActionHandler actionHandler;

    @InjectMocks
    private IONotificationService ioNotificationService;

    @Mock
    MomProducer<ActionEvent> actionsQueue;

    @Mock
    PnExternalRegistryIoClient pnExternalRegistryIoClient;

    @Mock
    PnDeliveryClient pnDeliveryClient;

    @Mock
    PnUserattributesConfig pnUserattributesConfig;

    @BeforeEach
    void setUp() {
        actionHandler = new ActionHandler(ioNotificationService);
    }

    @Test
    void pnUserAttributesSendMessageActionConsumer() {
        Consumer<Message<Action>> messageConsumer = actionHandler.pnUserAttributesSendMessageActionConsumer();
        assertNotNull(messageConsumer);
    }

    @Test
    void pnUserAttributesIoActivatedActionConsumer() {
        Consumer<Message<Action>> messageConsumer = actionHandler.pnUserAttributesIoActivatedActionConsumer();
        assertNotNull(messageConsumer);
    }

}