package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.exceptions.InternalErrorException;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.NotificationRecipient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.delivery.io.v1.dto.SentNotification;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.SendMessageRequest;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDeliveryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class IONotificationService   {

    private final MomProducer<ActionEvent> actionsQueue;
    private final PnExternalRegistryIoClient pnExternalRegistryIoClient;
    private final PnDeliveryClient pnDeliveryClient;
    private final int ioactivationSendolderthanDays;

    public IONotificationService(MomProducer<ActionEvent> actionsQueue,
                                 PnExternalRegistryIoClient pnExternalRegistryIoClient, PnDeliveryClient pnDeliveryClient, PnUserattributesConfig pnUserattributesConfig) {
        this.actionsQueue = actionsQueue;
        this.pnExternalRegistryIoClient = pnExternalRegistryIoClient;
        this.pnDeliveryClient = pnDeliveryClient;
        this.ioactivationSendolderthanDays = pnUserattributesConfig.getIoactivationSendolderthandays();
    }

    public Mono<Void> scheduleCheckNotificationToSendAfterIOActivation(String internalId, Instant lastDisabledStateTransitionTimestamp) {
        if (ioactivationSendolderthanDays<=0)
        {
            log.info("scheduleCheckNotificationToSendAfterIOActivation io activations is 0 or less, not scheduling io message activation check internalId={}", internalId);
            return Mono.empty();
        }
        else {
            return Mono.fromRunnable(() -> {
                try {
                    Instant checkFromWhen = Instant.now().minus(ioactivationSendolderthanDays, ChronoUnit.DAYS);
                    if (checkFromWhen.isBefore(lastDisabledStateTransitionTimestamp))
                    {
                        log.info("there is a disabled transition more recent than configured days, starting from that checkpoint");
                        checkFromWhen = lastDisabledStateTransitionTimestamp;
                    }

                    log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={} lastDisabledStateTransitionTimestamp={} checkFromWhen={}", internalId, lastDisabledStateTransitionTimestamp, checkFromWhen);
                    Action action = Action.builder()
                            .actionId(UUID.randomUUID().toString())
                            .internalId(internalId)
                            .checkFromWhen(checkFromWhen)
                            .timestamp(Instant.now())
                            .type(ActionType.IO_ACTIVATED_ACTION)
                            .build();

                    actionsQueue.push(ActionEvent.builder()
                            .header(StandardEventHeader.builder()
                                    .publisher("userAttributes")
                                    .eventId(action.getActionId())
                                    .iun(action.getActionId())
                                    .createdAt(Instant.now())
                                    .eventType(action.getType().name())
                                    .build()
                            )
                            .payload(action)
                            .build()
                    );
                } catch (Exception e) {
                    log.error("exception sending message", e);
                    throw e;
                }
            });
        }
    }

    public Mono<Void> scheduleSendMessage(String internalId, SentNotification sentNotification) {

        return Mono.fromRunnable(() -> {
            log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={}", internalId);
            Action action = Action.builder()
                    .actionId(UUID.randomUUID().toString())
                    .internalId(internalId)
                    .sentNotification(sentNotification)
                    .timestamp(Instant.now())
                    .type(ActionType.SEND_MESSAGE_ACTION)
                    .build();

            actionsQueue.push( ActionEvent.builder()
                    .header( StandardEventHeader.builder()
                            .publisher("userAttributes")
                            .eventId(action.getActionId())
                            .createdAt(Instant.now() )
                            .iun(action.getActionId())
                            .eventType(action.getType().name())
                            .build()
                    )
                    .payload( action )
                    .build()
            );
        });
    }


    public Mono<Void> consumeIoActivationEvent(String internalId, Instant checkFromWhen) {
        log.info("consumeIoActivationEvent internalId={} checkFromWhen={}", internalId, checkFromWhen);
        return Mono.defer(() -> this.pnDeliveryClient.searchNotificationPrivate(checkFromWhen.atOffset(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), internalId)
                .flatMap(sentNotification -> {
                    log.info("scheduling send IO Message for iun={} internalId={}", sentNotification.getIun(), internalId);
                    return this.scheduleSendMessage(internalId, sentNotification);
                })
                .switchIfEmpty(Mono.defer(() -> {
                      log.info("nothing to send  for internalId={}", internalId);
                      return Mono.empty();
                })).then());
    }


    public Mono<Void> consumeIoSendMessageEvent(String internalId, SentNotification sentNotification) {
        log.info("consumeIoSendMessageEvent iun={} internalId={}", sentNotification.getIun(), internalId);
        SendMessageRequest sendMessageRequest = this.getSendMessageRequest(sentNotification, internalId);
        return this.pnExternalRegistryIoClient.sendIOMessage(sendMessageRequest)
                .flatMap(res -> {
                    log.info("consumeIoSendMessageEvent sent message with result res={} iun={} internalId={}", res, sentNotification.getIun(), internalId);
                    return Mono.empty();
                });
    }


    @NotNull
    private SendMessageRequest getSendMessageRequest(SentNotification notification, String internalId) {

        Optional<NotificationRecipient> recipient = notification.getRecipients().stream().filter(rec -> rec.getInternalId().equals(internalId)).findFirst();
        if (recipient.isEmpty())
            throw new InternalErrorException();

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setAmount(notification.getAmount());
        if (notification.getPaymentExpirationDate() != null)
            sendMessageRequest.setDueDate(DateFormatUtils.parseDate(notification.getPaymentExpirationDate()).toOffsetDateTime());


        sendMessageRequest.setRecipientTaxID(recipient.get().getTaxId());

        sendMessageRequest.setSenderDenomination(notification.getSenderDenomination());
        sendMessageRequest.setIun(notification.getIun());
        sendMessageRequest.setSubject(notification.getSubject());

        if(recipient.get().getPayment() != null){
            sendMessageRequest.setNoticeNumber(recipient.get().getPayment().getNoticeCode());
            sendMessageRequest.setCreditorTaxId(recipient.get().getPayment().getCreditorTaxId());
        }else {
            log.warn("Recipient haven't payment information - iun={}", notification.getIun());
        }

        return sendMessageRequest;
    }
}
