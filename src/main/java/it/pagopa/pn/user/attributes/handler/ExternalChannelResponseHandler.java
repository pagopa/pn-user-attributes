package it.pagopa.pn.user.attributes.handler;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.LegalMessageSentDetailsDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalchannels.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Slf4j
@AllArgsConstructor
public class ExternalChannelResponseHandler {

    private final PnUserattributesConfig pnUserattributesConfig;
    private final AddressBookDao addressBookDao;


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
        // devo recuperare il record tramire il requestId, quindi purtroppo devo far una query ad-hoc
        return addressBookDao.getVerificationCodeByRequestId(requestId)
                .flatMap(verificationCodeEntity -> {

                    log.info("Saving pec valid flag for requestId={}", requestId);
                    verificationCodeEntity.setPecValid(true);

                    return addressBookDao.updateVerificationCodeIfExists(verificationCodeEntity)
                           .onErrorResume(throwable -> {
                               if (throwable instanceof ConditionalCheckFailedException ex)
                               {
                                   // l'errore non dovrebbe aver senso, ho fatto il check 3 istruzioni più su. Cmq lo assorbo
                                   log.error("Saving pec valid flag failed because item not found, probably by race condition, skipped save", ex);
                                   return Mono.empty();
                               }
                               return Mono.error(throwable);
                           });
                });
    }


}
