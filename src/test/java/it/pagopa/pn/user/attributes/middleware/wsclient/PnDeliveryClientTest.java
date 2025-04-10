package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.NotificationSearchResponse;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.NotificationSearchRow;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV25;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PnDeliveryClientTest {

    private InternalOnlyApi pnDeliveryApi;
    private PnDeliveryClient pnDeliveryClient;

    @BeforeEach
    void setUp() {
        pnDeliveryApi = Mockito.mock(InternalOnlyApi.class);
        pnDeliveryClient = new PnDeliveryClient(pnDeliveryApi);
    }

    @Test
    void searchNotificationPrivate_Success() {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(10);
        OffsetDateTime endDate = OffsetDateTime.now();
        String internalId = "testInternalId";

        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun("testIun");

        NotificationSearchRow notificationStatus = new NotificationSearchRow();
        notificationStatus.setIun("testIun");

        NotificationSearchResponse notificationSearchResponse = new NotificationSearchResponse();
        notificationSearchResponse.setResultsPage(List.of(notificationStatus));

        Mockito.when(pnDeliveryApi.searchNotificationsPrivate(
                Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.isNull(), Mockito.anyList(), Mockito.isNull(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.isNull()
        )).thenReturn(Mono.just(notificationSearchResponse));

        Mockito.when(pnDeliveryApi.getSentNotificationPrivate(Mockito.anyString()))
                .thenReturn(Mono.just(notification));

        List<SentNotificationV25> result = pnDeliveryClient.searchNotificationPrivate(startDate, endDate, internalId)
                .collectList()
                .block();

        assertEquals(1, result.size());
        assertEquals("testIun", result.get(0).getIun());
    }

    @Test
    void searchNotificationPrivate_Error() {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(10);
        OffsetDateTime endDate = OffsetDateTime.now();
        String internalId = "testInternalId";

        Mockito.when(pnDeliveryApi.searchNotificationsPrivate(
                Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.isNull(), Mockito.anyList(), Mockito.isNull(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.isNull()
        )).thenReturn(Mono.error(new RuntimeException("Test exception")));

        assertThrows(RuntimeException.class, () -> {
            pnDeliveryClient.searchNotificationPrivate(startDate, endDate, internalId).blockFirst();
        });
    }

    @Test
    void getSentNotificationPrivate_Success() {
        String iun = "testIun";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        Mockito.when(pnDeliveryApi.getSentNotificationPrivate(Mockito.anyString()))
                .thenReturn(Mono.just(notification));

        SentNotificationV25 result = pnDeliveryClient.getSentNotificationPrivate(iun).block();

        assertEquals(iun, result.getIun());
    }

    @Test
    void getSentNotificationPrivate_Error() {
        String iun = "testIun";

        Mockito.when(pnDeliveryApi.getSentNotificationPrivate(Mockito.anyString()))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));

        assertThrows(RuntimeException.class, () -> {
            pnDeliveryClient.getSentNotificationPrivate(iun).block();
        });
    }
}
