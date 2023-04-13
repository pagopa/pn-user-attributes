package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
        log.info("saving address in db uid:{} hashedaddress:{} channel:{} legal:{}", addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType(), addressBook.getAddressType());

        PnAuditLogEvent auditLogEvent = getLogEvent(addressBook);
        VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());
        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());

        return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity)
                .then(dao.deleteVerificationCode(verificationCodeEntity))
                .onErrorResume(x -> {
                    auditLogEvent.generateFailure("Error saving addressbook", x.getMessage()).log();
                    return Mono.error(x);
                })
                .doOnSuccess(x -> auditLogEvent.generateSuccess().log());
    }


    @NotNull
    private PnAuditLogEvent getLogEvent(AddressBookEntity addressBook) {
        String logMessage = String.format("save addressbook - recipientId=%s - senderId=%s - channelType=%s",
                addressBook.getRecipientId(), addressBook.getSenderId(), addressBook.getChannelType());

        PnAuditLogEventType auditLogEventType = PnAuditLogEventType.AUD_AB_DD_INSUP;
        if (Objects.equals(addressBook.getChannelType(), CourtesyChannelTypeDto.APPIO.getValue())) {
            if (addressBook.getAddresshash().equals(AddressBookEntity.APP_IO_ENABLED))
                auditLogEventType = PnAuditLogEventType.AUD_AB_DA_IO_INSUP;
            else
                auditLogEventType = PnAuditLogEventType.AUD_AB_DA_IO_DEL;
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