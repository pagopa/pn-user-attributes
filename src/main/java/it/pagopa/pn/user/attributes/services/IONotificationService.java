package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class IONotificationService   {

    private final MomProducer<ActionEvent> actionsQueue;
    private final PnExternalRegistryIoClient pnExternalRegistryIoClient;

    private final Clock clock;


    public IONotificationService(MomProducer<ActionEvent> actionsQueue,
                                 PnExternalRegistryIoClient pnExternalRegistryIoClient, Clock clock) {
        this.actionsQueue = actionsQueue;
        this.pnExternalRegistryIoClient = pnExternalRegistryIoClient;
        this.clock = clock;
    }

    public Mono<Void> scheduleCheckNotificationToSendAfterIOActivation(String internalId, Instant lastDisabledStateTransitionTimestamp) {

        return Mono.fromRunnable(() -> {
            log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={}", internalId);
            Action action = Action.builder()
                    .actionId(UUID.randomUUID().toString())
                    .internalId(internalId)
                    .lastDisabledStateTransitionTimestamp(lastDisabledStateTransitionTimestamp)
                    .timestamp(Instant.now())
                    .type(ActionType.IO_ACTIVATED_ACTION)
                    .build();

            actionsQueue.push( ActionEvent.builder()
                    .header( StandardEventHeader.builder()
                            .publisher("userAttributes")
                            .eventId(action.getActionId())
                            .createdAt( clock.instant() )
                            .eventType(action.getType().name())
                            .build()
                    )
                    .payload( action )
                    .build()
            );
        });
    }

    public Mono<Void> scheduleSendMessage(String internalId) {

        return Mono.fromRunnable(() -> {
            log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={}", internalId);
            Action action = Action.builder()
                    .actionId(UUID.randomUUID().toString())
                    .internalId(internalId)
                    .timestamp(Instant.now())
                    .type(ActionType.SEND_MESSAGE_ACTION)
                    .build();

            actionsQueue.push( ActionEvent.builder()
                    .header( StandardEventHeader.builder()
                            .publisher("userAttributes")
                            .eventId(action.getActionId())
                            .createdAt( clock.instant() )
                            .eventType(action.getType().name())
                            .build()
                    )
                    .payload( action )
                    .build()
            );
        });
    }


    public Mono<Void> processIoActivation(String internalId) {

        return Mono.fromRunnable(() -> {
            log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={}", internalId);
            Action action = Action.builder()
                    .actionId(UUID.randomUUID().toString())
                    .internalId(internalId)
                    .timestamp(Instant.now())
                    .type(ActionType.SEND_MESSAGE_ACTION)
                    .build();

            actionsQueue.push( ActionEvent.builder()
                    .header( StandardEventHeader.builder()
                            .publisher("userAttributes")
                            .eventId(action.getActionId())
                            .createdAt( clock.instant() )
                            .eventType(action.getType().name())
                            .build()
                    )
                    .payload( action )
                    .build()
            );
        });
    }

/*
    @NotNull
    private SendMessageRequest getSendMessageRequest(Notification notification, NotificationRecipientInt recipientInt) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setAmount(notification.getAmount());
        sendMessageRequest.setDueDate(notification.getPaymentExpirationDate());
        sendMessageRequest.setRecipientTaxID(recipientInt.getTaxId());

        sendMessageRequest.setSenderDenomination(notification.getSender().getPaDenomination());
        sendMessageRequest.setIun(notification.getIun());
        sendMessageRequest.setSubject(notification.getSubject());

        if(recipientInt.getPayment() != null){
            sendMessageRequest.setNoticeNumber(recipientInt.getPayment().getNoticeCode());
            sendMessageRequest.setCreditorTaxId(recipientInt.getPayment().getCreditorTaxId());
        }else {
            log.warn("Recipient haven't payment information - iun={}", notification.getIun());
        }

        return sendMessageRequest;
    }*/
}
