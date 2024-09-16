package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.dto.SingleStatusUpdateDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Component
public class ExternalChannelResponseHandler {

    private final PnUserattributesConfig pnUserattributesConfig;
    private final AddressBookDao addressBookDao;
    private final VerificationCodeUtils verificationCodeUtils;
    private final PnExternalChannelClient externalChannelClient;

    private static final String PEC_CONFIRM_PREFIX = "pec-confirm-";


    public Mono<Void> consumeExternalChannelResponse(SingleStatusUpdateDto singleStatusUpdateDto) {
        if (singleStatusUpdateDto.getDigitalLegal() != null)
        {
            LegalMessageSentDetailsDto legalMessageSentDetailsDto = singleStatusUpdateDto.getDigitalLegal();
            if (pnUserattributesConfig.getExternalChannelDigitalCodesSuccess().contains(legalMessageSentDetailsDto.getEventCode()))
            {
                // è una conferma di invio PEC.
                // cerco il verification code da aggiornare e setto il flag di PEC inviata.
                // se non lo trovo, loggo e ignoro perchè vuol dire che è la conferma è arrivata "tardi".
                log.info("Arrived legal singleStatusUpdateDto from external channel, and is SUCCESS code, saving PEC flag singleStatusUpdateDto={}", singleStatusUpdateDto);
                return checkVerificationAddressAndSave(legalMessageSentDetailsDto.getRequestId());
            }
            else {
                log.info("Arrived legal singleStatusUpdateDto from external channel, but not an success code, nothig to do singleStatusUpdateDto={}", singleStatusUpdateDto);
                return Mono.empty();
            }
        }
        else {
            log.info("Arrived courtesy singleStatusUpdateDto from external channel, nothing to to singleStatusUpdateDto={}", singleStatusUpdateDto);
            return Mono.empty();
        }
    }

    private Mono<Void> checkVerificationAddressAndSave(String requestId) {

        if (requestId.startsWith(PEC_CONFIRM_PREFIX))
        {
            log.info("RequestId has pec-confirm prefix, nothing to do requestId={}", requestId);
            return Mono.empty();
        }

        String logMessage = String.format(
                "checkVerificationAddressAndSave PEC sending verification code requestId=%s", requestId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_AB_VALIDATE_PEC, logMessage)
                .build();
        logEvent.log();

        // devo recuperare il record tramire il requestId, quindi purtroppo devo far una query ad-hoc
        return addressBookDao.getVerificationCodeByRequestId(requestId)
                .flatMap(verificationCodeEntity -> {

                    if (verificationCodeEntity.isCodeValid()) {
                        // se il codice di verifica è valido posso procedere con il salvare l'indirizzo PEC
                        return verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCodeEntity, null)
                                .flatMap(x -> externalChannelClient.sendPecConfirm(PEC_CONFIRM_PREFIX + requestId, verificationCodeEntity.getRecipientId(), verificationCodeEntity.getAddress()))
                                .doOnSuccess(x -> logEvent.generateSuccess("Pec verified successfully recipientId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress()).log())
                                .thenReturn("OK");
                    } else {
                        // altrimenti salvo semplicemente il flag
                        return verificationCodeUtils.markVerificationCodeAsPecValid(verificationCodeEntity)
                                .doOnSuccess(x -> logEvent.generateSuccess("Pec verified successfully recipientId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress()).log())
                                .thenReturn("OK");
                    }
                })
                .switchIfEmpty(Mono.fromRunnable(
                        () -> logEvent.generateWarning("No pending VerifiedCode for requestId").log()).thenReturn("KO"))
                .onErrorResume(x -> {
                    String message = x.getMessage();
                    String failureMessage = String.format("checkVerificationAddressAndSave PEC error %s", message);
                    logEvent.generateFailure(failureMessage).log();
                    log.error("checkVerificationAddressAndSave PEC error {}", message, x);
                    return Mono.error(x);
                })
                .then();
    }


}
