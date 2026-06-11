package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
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

        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj", null).block());

    }


    @Test
    void consumePecValidationExpiredEventException() {

        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.error(new RuntimeException("aaaa")));

        Assertions.assertThrows(Exception.class, () -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj", null).block());

    }

    @Test
    void consumePecValidationExpiredEvent_languageDE_propagatesDE() {
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj", "DE").block());

        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(LanguageEnum.DE));
    }

    @Test
    void consumePecValidationExpiredEvent_languageNull_fallbackToIT() {
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj", null).block());

        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(LanguageEnum.IT));
    }

    @Test
    void consumePecValidationExpiredEvent_languageUnsupported_fallbackToIT() {
        Mockito.when(pnExternalChannelClient.sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        Assertions.assertDoesNotThrow(() -> pecValidationExpiredResponseHandler.consumePecValidationExpiredEvent("fghjkl", "435678@fghj", "XX").block());

        Mockito.verify(pnExternalChannelClient).sendCourtesyPecRejected(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(LanguageEnum.IT));
    }
}