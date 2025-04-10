package it.pagopa.pn.user.attributes.middleware.queue.consumer;

import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.CourtesyMessageProgressEventDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalChannelHandlerTest {

    private ExternalChannelHandler externalChannelHandler;

    @Mock
    private ExternalChannelResponseHandler externalChannelResponseHandler;

    @BeforeEach
    void setUp() {
        externalChannelResponseHandler = Mockito.mock(ExternalChannelResponseHandler.class);
        externalChannelHandler = new ExternalChannelHandler(externalChannelResponseHandler);
    }

    @Test
    void pnExternalChannelEventConsumer() {
        Consumer<Message<SingleStatusUpdateDto>> messageConsumer = externalChannelHandler.pnExternalChannelEventConsumer();
        assertNotNull(messageConsumer);

        Mockito.when(externalChannelResponseHandler.consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class)))
                .thenReturn(Mono.empty());

        Message<SingleStatusUpdateDto> message = new Message<>() {
            @Override
            public SingleStatusUpdateDto getPayload() {
                SingleStatusUpdateDto payload = new SingleStatusUpdateDto();
                LegalMessageSentDetailsDto legalMessage = new LegalMessageSentDetailsDto();
                legalMessage.setRequestId("123456");
                payload.setDigitalLegal(legalMessage);
                return payload;
            }

            @Override
            public MessageHeaders getHeaders() {
                return new MessageHeaders(Map.of());
            }
        };

        messageConsumer.accept(message);

        Mockito.verify(externalChannelResponseHandler, Mockito.times(1))
                .consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class));
    }

    @Test
    void pnExternalChannelEventConsumer_withCourtesyMessage() {
        Consumer<Message<SingleStatusUpdateDto>> messageConsumer = externalChannelHandler.pnExternalChannelEventConsumer();
        assertNotNull(messageConsumer);

        Mockito.when(externalChannelResponseHandler.consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class)))
                .thenReturn(Mono.empty());

        Message<SingleStatusUpdateDto> message = new Message<>() {
            @Override
            public SingleStatusUpdateDto getPayload() {
                SingleStatusUpdateDto payload = new SingleStatusUpdateDto();
                CourtesyMessageProgressEventDto courtesyMessage = new CourtesyMessageProgressEventDto();
                courtesyMessage.setRequestId("123456");
                payload.setDigitalCourtesy(courtesyMessage);
                return payload;
            }

            @Override
            public MessageHeaders getHeaders() {
                return new MessageHeaders(Map.of());
            }
        };

        messageConsumer.accept(message);

        Mockito.verify(externalChannelResponseHandler, Mockito.times(1))
                .consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class));
    }

    @Test
    void pnExternalChannelEventConsumer_catchException() {
        Consumer<Message<SingleStatusUpdateDto>> messageConsumer = externalChannelHandler.pnExternalChannelEventConsumer();

        Mockito.when(externalChannelResponseHandler.consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class)))
                .thenThrow(new RuntimeException("Simulated exception"));

        Message<SingleStatusUpdateDto> message = new Message<>() {
            @Override
            public SingleStatusUpdateDto getPayload() {
                SingleStatusUpdateDto payload = new SingleStatusUpdateDto();
                LegalMessageSentDetailsDto legalMessage = new LegalMessageSentDetailsDto();
                legalMessage.setRequestId("123456");
                payload.setDigitalLegal(legalMessage);
                return payload;
            }

            @Override
            public MessageHeaders getHeaders() {
                return new MessageHeaders(Map.of());
            }
        };

        assertThrows(RuntimeException.class, () -> messageConsumer.accept(message));

        Mockito.verify(externalChannelResponseHandler, Mockito.times(1))
                .consumeExternalChannelResponse(Mockito.any(SingleStatusUpdateDto.class));
    }
}