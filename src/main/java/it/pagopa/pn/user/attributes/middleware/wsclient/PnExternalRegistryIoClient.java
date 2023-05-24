package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.services.AuditLogService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.IoActivationApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.SendIoMessageApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnExternalRegistryIoClient extends CommonBaseClient {

    private final IoActivationApi ioApi;
    private final SendIoMessageApi ioMessageApi;
    private final PnDataVaultClient pnDataVaultClient;
    private final AuditLogService auditLogService;

    public PnExternalRegistryIoClient(IoActivationApi ioApi, SendIoMessageApi ioMessageApi, PnDataVaultClient pnDataVaultClient, AuditLogService auditLogService) {
        this.ioApi = ioApi;
        this.ioMessageApi = ioMessageApi;
        this.pnDataVaultClient = pnDataVaultClient;
        this.auditLogService = auditLogService;
    }

    /**
     * Crea (o aggiorna) lo stato in IO
     *
     * @param internalId internalId dell'utente
     * @param activated indica se attivato o disattivato
     *
     * @return void
     */
    public Mono<Boolean> upsertServiceActivation(String internalId, boolean activated)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Upsert app IO activation");
        log.debug("upsertServiceActivation internalId={} activated={}", internalId, activated);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    ActivationPayload dto = new ActivationPayload();
                    dto.setFiscalCode(user.getTaxId());
                    dto.setStatus(activated? ActivationStatus.ACTIVE : ActivationStatus.INACTIVE);

                    return ioApi.upsertServiceActivation(dto)
                            .onErrorResume(throwable -> {
                                log.error("error upserting service activation message={}", elabExceptionMessage(throwable) , throwable);
                                return getServiceActivation(internalId);
                            })
                            .map(x -> {
                                log.info("upsertServiceActivation response taxid={} status={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getVersion());
                                return x.getStatus().equals(ActivationStatus.ACTIVE);
                            });
                });
    }

    public Mono<UserStatusResponse> checkValidUsers(String internalId) throws WebClientResponseException {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Check app IO user status");
        log.debug("checkValidUsers internalId={}", internalId);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    UserStatusRequest userStatusRequest = new UserStatusRequest();
                    userStatusRequest.setTaxId(user.getTaxId());
                    return this.ioMessageApi.userStatus(userStatusRequest);
                });
    }

    /**
     * Ritorna lo stato lo stato in IO
     *
     * @param internalId internalId dell'utente
     *
     * @return void
     */
    public Mono<Activation> getServiceActivation(String internalId)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Retrieving app IO activation");
        log.debug("getServiceActivation internalId={}", internalId);

        return this.pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId))
                .take(1).next()
                .flatMap(user -> {
                    FiscalCodePayload dto = new FiscalCodePayload();
                    dto.setFiscalCode(user.getTaxId());

                    return ioApi.getServiceActivationByPOST(dto)
                            .map(x -> {
                                log.info("getServiceActivation response taxid={} status={} version={}", LogUtils.maskTaxId(x.getFiscalCode()), x.getStatus(), x.getVersion());
                                return x;
                            });
                });
    }

    public Mono<SendMessageResponse> sendIOMessage(SendMessageRequest sendMessageRequest) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Sending app IO message");
        log.debug("sendIOMessage sendMessageRequest iun={}", sendMessageRequest.getIun());

        final PnAuditLogEvent logEvent = auditLogService.buildAuditLogEventWithIUN(sendMessageRequest.getIun(),
                sendMessageRequest.getRecipientIndex(), PnAuditLogEventType.AUD_DA_SEND_IO, "sendIOMessage after io activated");

        return this.ioMessageApi.sendIOMessage(sendMessageRequest)
                .onErrorResume(throwable -> {
                    logEvent.generateFailure("error sending message to ext-registry for IO exc={}", throwable).log();
                    return Mono.error(throwable);
                })
                .map(res -> {
                    if (res.getResult() == SendMessageResponse.ResultEnum.ERROR_COURTESY
                        || res.getResult() == SendMessageResponse.ResultEnum.ERROR_OPTIN
                        || res.getResult() == SendMessageResponse.ResultEnum.ERROR_USER_STATUS)
                        logEvent.generateFailure("Send message with ERROR outcome={} id={}", res.getResult(), res.getId()).log();
                    else
                        logEvent.generateSuccess("Send message outcome={} id={}", res.getResult(), res.getId()).log();
                    return res;
                });
    }


}
