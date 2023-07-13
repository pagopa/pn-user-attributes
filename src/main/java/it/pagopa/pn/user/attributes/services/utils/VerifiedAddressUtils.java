package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CourtesyChannelTypeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerifiedAddressUtils {

    private final AddressBookDao dao;


    /**
     * Salva in dynamodb l'id offuscato
     *
     * @param addressBook addressBook da salvare, COMPLETO di hashedaddress impostato
     * @return nd
     */
    public Mono<Void> saveInDynamodb(AddressBookEntity addressBook){
        log.info("saving address in db uid={} hashedaddress={} channel={} legal={}", addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType(), addressBook.getAddressType());

        PnAuditLogEvent auditLogEvent = getLogEvent(addressBook);
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());
        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());

        return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity)
                .then(dao.deleteVerificationCode(verificationCodeEntity))
                .onErrorResume(x -> {
                    if (auditLogEvent != null)
                        auditLogEvent.generateFailure("Error saving addressbook", x.getMessage()).log();
                    else
                        log.error("error saving address book", x);
                    return Mono.error(x);
                })
                .doOnSuccess(x -> {
                    if (auditLogEvent != null)
                        auditLogEvent.generateSuccess().log();
                    else
                        log.info("address book saved successfully uid={} hashedaddress={} channel={} legal={}", addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType(), addressBook.getAddressType());
                });
    }


    private PnAuditLogEvent getLogEvent(AddressBookEntity addressBook) {
        String logMessage = String.format("save addressbook - recipientId=%s - senderId=%s - channelType=%s - hashedAddress=%s",
                addressBook.getRecipientId(), addressBook.getSenderId(), addressBook.getChannelType(), addressBook.getAddresshash());

        PnAuditLogEventType auditLogEventType = PnAuditLogEventType.AUD_AB_DD_INSUP;
        if (Objects.equals(addressBook.getChannelType(), CourtesyChannelTypeDto.APPIO.getValue())) {
            // per appIO, il log di evento è più complesso, perchè il "success" deve includere l'invocazione del BE di IO
            return null;
        }
        if (Objects.equals(addressBook.getChannelType(), CourtesyChannelTypeDto.SMS.getValue())
            || Objects.equals(addressBook.getChannelType(), CourtesyChannelTypeDto.EMAIL.getValue()))
            auditLogEventType = PnAuditLogEventType.AUD_AB_DA_INSUP;

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(auditLogEventType, logMessage)
                .build();
        logEvent.log();
        return logEvent;
    }

}