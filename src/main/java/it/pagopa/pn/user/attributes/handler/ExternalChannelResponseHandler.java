package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalChannelClient;
import it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class ExternalChannelResponseHandler {

    private final PnUserattributesConfig pnUserattributesConfig;
    private final AddressBookDao addressBookDao;
    private final VerificationCodeUtils verificationCodeUtils;
    private final PnExternalChannelClient externalChannelClient;


    public Mono<Void> consumeExternalChannelResponse(SingleStatusUpdateDto singleStatusUpdateDto) {
        if (singleStatusUpdateDto.getDigitalLegal() != null)
        {
            LegalMessageSentDetailsDto legalMessageSentDetailsDto = singleStatusUpdateDto.getDigitalLegal();
            if (pnUserattributesConfig.getExternalchannelDigitalCodesSuccess().contains(legalMessageSentDetailsDto.getEventCode()))
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
                        return verificationCodeUtils.sendToDataVaultAndSaveInDynamodb(verificationCodeEntity, verificationCodeEntity.getAddress())
                                .flatMap(x -> externalChannelClient.sendPecConfirm(verificationCodeEntity.getRecipientId(), verificationCodeEntity.getAddress()))
                                .doOnSuccess(x -> logEvent.generateSuccess("Pec verified successfully recipientId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress()).log())
                                .then();
                    } else {
                        // altrimenti salvo semplicemente il flag
                        return verificationCodeUtils.markVerificationCodeAsPecValid(verificationCodeEntity)
                                .doOnSuccess(x -> logEvent.generateSuccess("Pec verified successfully recipientId={} hashedAddress={}", verificationCodeEntity.getRecipientId(), verificationCodeEntity.getHashedAddress()).log())
                                .then();
                    }
                })
                .switchIfEmpty(Mono.fromRunnable(
                        () -> logEvent.generateWarning("No pending VerifiedCode for requestId").log()).then())
                .onErrorResume(x -> {
                    String message = x.getMessage();
                    String failureMessage = String.format("checkVerificationAddressAndSave PEC error %s", message);
                    logEvent.generateFailure(failureMessage).log();
                    log.error("checkVerificationAddressAndSave PEC error {}", message, x);
                    return Mono.error(x);
                });
    }


}
