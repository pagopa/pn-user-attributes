package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.services.AuditLogService;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.IoActivationApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.SendIoMessageApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Classe wrapper di io-external-channel, con gestione del backoff
 */
@Component
@Slf4j
public class PnExternalRegistryIoClient extends CommonBaseClient {

    private IoActivationApi ioApi;
    private SendIoMessageApi ioMessageApi;
    private final PnUserattributesConfig pnUserattributesConfig;
    private final PnDataVaultClient pnDataVaultClient;
    private final AuditLogService auditLogService;

    public PnExternalRegistryIoClient(PnUserattributesConfig pnUserattributesConfig, PnDataVaultClient pnDataVaultClient, AuditLogService auditLogService) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.pnDataVaultClient = pnDataVaultClient;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        this.ioApi = new IoActivationApi(apiClient);

        apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        this.ioMessageApi = new SendIoMessageApi(apiClient);
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
        log.info("upsertServiceActivation internalId={} activated={}", internalId, activated);

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
        log.info("getServiceActivation internalId={}", internalId);

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
        log.info("sendIOMessage sendMessageRequest iun={}", sendMessageRequest.getIun());

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
