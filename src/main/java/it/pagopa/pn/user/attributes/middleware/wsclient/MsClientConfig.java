package it.pagopa.pn.user.attributes.middleware.wsclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.IoActivationApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.api.SendIoMessageApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.AooUoIdsApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.InfoPaApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.api.RootSenderIdApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsClientConfig  extends CommonBaseClient {

    @Bean
    AddressBookApi addressBookApi(PnUserattributesConfig pnUserattributesConfig) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        return new AddressBookApi(apiClient);
    }

    @Bean
    RecipientsApi recipientsApi(PnUserattributesConfig pnUserattributesConfig) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        return new RecipientsApi(apiClient);
    }

    @Bean
    InternalOnlyApi deliveryInternalOnlyApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.delivery.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDeliveryBasepath());

        return new InternalOnlyApi(apiClient);
    }


    @Bean
    DigitalLegalMessagesApi digitalLegalMessagesApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalchannelsBasepath());

        return new DigitalLegalMessagesApi(apiClient);
    }


    @Bean
    DigitalCourtesyMessagesApi digitalCourtesyMessagesApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalchannels.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalchannelsBasepath());

        return new DigitalCourtesyMessagesApi(apiClient);
    }


    @Bean
    RootSenderIdApi rootSenderIdApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());
        return new RootSenderIdApi(apiClient);
    }

    @Bean
    AooUoIdsApi aooUoIdsApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());
        return new AooUoIdsApi(apiClient);
    }

    @Bean
    IoActivationApi ioActivationApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        return new IoActivationApi(apiClient);
    }

    @Bean
    SendIoMessageApi sendIoMessageApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());

        return new SendIoMessageApi(apiClient);
    }


    @Bean
    InfoPaApi infoPaApi(PnUserattributesConfig pnUserattributesConfig) {
        it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient apiClient = new it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient(initWebClient(it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientExternalregistryBasepath());
        return new InfoPaApi(apiClient);
    }

}
