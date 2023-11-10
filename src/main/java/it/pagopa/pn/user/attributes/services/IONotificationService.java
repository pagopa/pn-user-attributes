package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.queue.entities.Action;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionEvent;
import it.pagopa.pn.user.attributes.middleware.queue.entities.ActionType;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnDeliveryClient;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryIoClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.NotificationRecipientV21;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.dto.SentNotificationV21;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.SendMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_MISSING_RECIPIENTID;

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
                        log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={} there is a disabled transition more recent than configured days, starting from that checkpoint lastDisabledStateTransitionTimestamp={}", internalId, lastDisabledStateTransitionTimestamp);
                        checkFromWhen = lastDisabledStateTransitionTimestamp;
                    }
                    else {
                        log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={} lastDisabledStateTransitionTimestamp={} checkFromWhen={}", internalId, lastDisabledStateTransitionTimestamp, checkFromWhen);
                    }

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

    public Mono<Void> scheduleSendMessage(String actionIdPrefix, String internalId, SentNotificationV21 sentNotification) {

        return Mono.fromRunnable(() -> {
            log.info("scheduleCheckNotificationToSendAfterIOActivation internalId={}", internalId);
            Action action = Action.builder()
                    .actionId(actionIdPrefix +"_"+ UUID.randomUUID())
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


    public Mono<Void> consumeIoActivationEvent(String actionId, String internalId, Instant checkFromWhen) {
        log.info("consumeIoActivationEvent internalId={} checkFromWhen={}", internalId, checkFromWhen);
        return Mono.defer(() -> this.pnDeliveryClient.searchNotificationPrivate(checkFromWhen.atOffset(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), internalId)
                .flatMap(sentNotification -> {
                    log.info("scheduling send IO Message for iun={} internalId={}", sentNotification.getIun(), internalId);
                    return this.scheduleSendMessage(actionId, internalId, sentNotification);
                })
                .switchIfEmpty(Mono.defer(() -> {
                      log.info("nothing to send  for internalId={}", internalId);
                      return Mono.empty();
                })).then());
    }


    public Mono<Void> consumeIoSendMessageEvent(String internalId, SentNotificationV21 sentNotification) {
        log.debug("consumeIoSendMessageEvent iun={} internalId={}", sentNotification.getIun(), internalId);
        SendMessageRequest sendMessageRequest = this.getSendMessageRequest(sentNotification, internalId);
        return this.pnExternalRegistryIoClient.sendIOMessage(sendMessageRequest)
                .flatMap(res -> {
                    log.info("consumeIoSendMessageEvent sent message with result res={} iun={} internalId={}", res, sentNotification.getIun(), internalId);
                    return Mono.empty();
                });
    }


    @NotNull
    private SendMessageRequest getSendMessageRequest(SentNotificationV21 notification, String internalId) {

        // recupero l'indice del recipient, mi servirà poi
        OptionalInt indexRecipient = IntStream.range(0, notification.getRecipients().size())
                .filter(i -> internalId.equals(notification.getRecipients().get(i).getInternalId()))
                .findFirst();

        if (indexRecipient.isEmpty())
            throw new PnInternalException("recipient is empty", ERROR_CODE_MISSING_RECIPIENTID);

        NotificationRecipientV21 recipient = notification.getRecipients().get(indexRecipient.getAsInt());

        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setAmount(notification.getAmount());
        if (notification.getPaymentExpirationDate() != null)
            sendMessageRequest.setDueDate(DateFormatUtils.parseDate(notification.getPaymentExpirationDate()).toOffsetDateTime());
        sendMessageRequest.setRequestAcceptedDate(notification.getSentAt());

        sendMessageRequest.setRecipientTaxID(recipient.getTaxId());
        sendMessageRequest.setRecipientInternalID(internalId);
        sendMessageRequest.setRecipientIndex(indexRecipient.getAsInt());

        // voglio inviare il CC
        sendMessageRequest.setCarbonCopyToDeliveryPush(true);

        sendMessageRequest.setSenderDenomination(notification.getSenderDenomination());
        sendMessageRequest.setIun(notification.getIun());

        sendMessageRequest.setSubject(prepareSubjectForIO(notification.getSenderDenomination(), notification.getSubject()));

        return sendMessageRequest;
    }

    private String prepareSubjectForIO(String senderDenomination, String subject) {
        // tronca il nome della PA se oltre i 50 caratteri, per lasciare spazio all'oggetto
        // della notifica (IO supporta max 120 caratteri)
        if (senderDenomination == null)
            return subject;

        if (senderDenomination.length() > 50)
            senderDenomination = senderDenomination.substring(0,47) + "...";

        return senderDenomination +" - "+ subject;
    }
}
