package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.NotificationRecipient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.SendMessageResponse;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDeliveryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class IONotificationServiceTest {

    private final Duration d = Duration.ofMillis(3000);



    @Autowired
    private IONotificationService service;

    @MockBean
    private MomProducer< ActionEvent > actionsQueue;

    @MockBean
    PnUserattributesConfig pnUserattributesConfig;

    @MockBean
    PnExternalRegistryIoClient pnExternalRegistryIoClient;

    @MockBean
    PnDeliveryClient pnDeliveryClient;


    @Test
    void scheduleCheckNotificationToSendAfterIOActivation() {
        //GIVEN
        String recipientId = "recipientid";
        SentNotification sentNotification = new SentNotification();

        Mockito.when(pnUserattributesConfig.getIoactivationSendolderthandays()).thenReturn(7);
        //Mockito.when(this.actionsQueue.push(Mockito.any(ActionEvent.class)));


        // WHEN
        Mono<Void> mono = service.scheduleCheckNotificationToSendAfterIOActivation(recipientId, Instant.now());
        assertDoesNotThrow(() -> {
            mono.block(d);
        });

    }

    @Test
    void consumeIoActivationEvent() {
        //GIVEN
        String recipientId = "recipientid";
        SentNotification sentNotification = new SentNotification();

        Mockito.when(this.pnDeliveryClient.searchNotificationPrivate(Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(Flux.fromIterable(List.of(sentNotification)));


        // WHEN
        Mono<Void> mono = service.consumeIoActivationEvent(recipientId, Instant.now());
        assertDoesNotThrow(() -> {
            mono.block(d);
        });


        //THEN
    }

    @Test
    void consumeIoSendMessageEvent() {
        //GIVEN
        String recipientId = "recipientid";
        SentNotification sentNotification = new SentNotification();
        sentNotification.setRecipients(new ArrayList<>());
        sentNotification.getRecipients().add(new NotificationRecipient());
        sentNotification.getRecipients().get(0).setInternalId(recipientId);

        SendMessageResponse sendMessageResponse = new SendMessageResponse();
        sendMessageResponse.setId("23423423423");

        Mockito.when(this.pnExternalRegistryIoClient.sendIOMessage(Mockito.any())).thenReturn(Mono.just(sendMessageResponse));

        // WHEN
        Mono<Void> mono = service.consumeIoSendMessageEvent(recipientId, sentNotification);
        assertDoesNotThrow(() -> {
            mono.block(d);
        });


        //THEN

    }
}