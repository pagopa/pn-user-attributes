package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.LegalChannelTypeDto;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.externalchannels.v1.api.DigitalLegalMessagesApi;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
public class PnExternalChannelClient extends BaseClient {

    private final PnUserattributesConfig pnUserattributesConfig;
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private DigitalLegalMessagesApi digitalLegalMessagesApi;


    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig, DigitalCourtesyMessagesApi digitalCourtesyMessagesApi, DigitalLegalMessagesApi digitalLegalMessagesApi) {
        this.pnUserattributesConfig = pnUserattributesConfig;
        this.digitalCourtesyMessagesApi = digitalCourtesyMessagesApi;
        this.digitalLegalMessagesApi = digitalLegalMessagesApi;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        this.digitalLegalMessagesApi = new DigitalLegalMessagesApi(apiClient);

        apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        this.digitalCourtesyMessagesApi = new DigitalCourtesyMessagesApi(apiClient);
    }


    public Mono<Void> sendVerificationCode(String address, LegalChannelTypeDto legalChannelType, CourtesyChannelTypeDto courtesyChannelType, String verificationCode)
    {
        //digitalCourtesyMessagesApi.sendCourtesyShortMessage()
        return Mono.empty();
            
    }

}
