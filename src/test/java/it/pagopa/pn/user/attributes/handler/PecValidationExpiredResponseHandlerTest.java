package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PecValidationExpiredResponseHandlerTest {
    @InjectMocks
    private PecValidationExpiredResponseHandler pecValidationExpiredResponseHandler;

    @Mock
    private PnExternalChannelClient pnExternalChannelClient;



    @Test
    void consumePecValidationExpiredEvent() {

        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj").block());

    }


    @Test
    void consumePecValidationExpiredEventException() {

        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.error(new RuntimeException("aaaa")));

        Assertions.assertThrows(Exception.class, () -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj").block());

    }
}