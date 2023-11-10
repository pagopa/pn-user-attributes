package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.handler.PecValidationExpiredResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDeliveryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import it.pagopa.pn.user.attributes.services.IONotificationService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV21;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActionHandlerTest {

    private ActionHandler actionHandler;

    @InjectMocks
    private IONotificationService ioNotificationService;
    @InjectMocks
    private PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler;

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
        ioNotificationService = Mockito.mock(IONotificationService.class);
        pecValidationExpiredResponseHandler = Mockito.mock((PecValidationExpiredResponseHandler.class));
        actionHandler = new ActionHandler(ioNotificationService, pecValidationExpiredResponseHandler);
    }

    @Test
    void pnUserAttributesSendMessageActionConsumer() {
        Consumer<Message<Action>> messageConsumer = actionHandler.pnUserAttributesSendMessageActionConsumer();
        assertNotNull(messageConsumer);

        Mockito.when(ioNotificationService.consumeIoSendMessageEvent(Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        Message<Action> message = new Message<Action>() {
            @Override
            public Action getPayload() {
                Action action = Action.builder()
                        .internalId("123")
                        .actionId("123456")
                        .type(ActionType.SEND_MESSAGE_ACTION)
                        .sentNotification(new SentNotificationV21())
                        .build();
                return action;
            }

            @Override
            public MessageHeaders getHeaders() {
                MessageHeaders messageHeaders = new MessageHeaders(Map.of());
                return messageHeaders;
            }
        };

        messageConsumer.accept(message);
    }

    @Test
    void pnUserAttributesIoActivatedActionConsumer() {
        Consumer<Message<Action>> messageConsumer = actionHandler.pnUserAttributesIoActivatedActionConsumer();
        assertNotNull(messageConsumer);
        Mockito.when(ioNotificationService.consumeIoActivationEvent(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        Message<Action> message = new Message<Action>() {
            @Override
            public Action getPayload() {
                Action action = Action.builder()
                        .internalId("123")
                        .actionId("123456")
                        .type(ActionType.IO_ACTIVATED_ACTION)
                        .build();
                return action;
            }

            @Override
            public MessageHeaders getHeaders() {
                MessageHeaders messageHeaders = new MessageHeaders(Map.of());
                return messageHeaders;
            }
        };

        messageConsumer.accept(message);
    }
    @Test
    void pnUserAttributesPecValidationExpiredActionConsumer() {
        Consumer<Message<Action>> messageConsumer = actionHandler.pnUserAttributesPecValidationExpiredActionConsumer();
        assertNotNull(messageConsumer);
        Mockito.when(pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent(Mockito.any(), Mockito.anyString())).thenReturn(Mono.empty());
        Message<Action> message = new Message<Action>() {
            @Override
            public Action getPayload() {
                Action action = Action.builder()
                        .internalId("123")
                        .actionId("123456")
                        .type(ActionType.PEC_REJECTED_ACTION)
                        .address("email@email.it")
                        .build();
                return action;
            }

            @Override
            public MessageHeaders getHeaders() {
                MessageHeaders messageHeaders = new MessageHeaders(Map.of());
                return messageHeaders;
            }
        };

        messageConsumer.accept(message);
    }
}