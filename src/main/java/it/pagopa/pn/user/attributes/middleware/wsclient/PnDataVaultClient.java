package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
public class PnDataVaultClient extends BaseClient {
    
    private AddressBookApi addressBookApi;
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnDataVaultClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        this.addressBookApi = new AddressBookApi(apiClient);
    }

    /**
     * Crea (o aggiorna) un indirizzo
     *
     * @param internalId uid dell'utente
     * @param addressId uid dell'indirizzo
     * @param realaddress valore dell'indirizzo
     *
     * @return void
     */
    public Mono<Void> updateRecipientAddressByInternalId(String internalId, String addressId, String realaddress)
    {
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(realaddress);
        return addressBookApi.updateRecipientAddressByInternalId (internalId, addressId, dto)
            .retryWhen(
                    Retry.backoff(2, Duration.ofMillis(25))
                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );                             
            
    }

    /**
     * Recupera gli indirizzi per un utente
     *
     * @param internalId uid dell'utente
     *
     * @return la lista degli indirizzi
     */
    public Mono<RecipientAddressesDtoDto> getRecipientAddressesByInternalId(String internalId)
    {
        return addressBookApi.getRecipientAddressesByInternalId (internalId)
            .retryWhen(
                    Retry.backoff(2, Duration.ofMillis(25))
                            .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );
            
    }

    /**
     * Elimina un indirizzo precedentemente salvato per un utente
     *
     * @param internalId uid dell'utente
     * @param addressId uid dell'indirizzo
     *
     * @return void
     */
    public Mono<Void> deleteRecipientAddressByInternalId(String internalId, String addressId)
    {
        return addressBookApi.deleteRecipientAddressByInternalId  (internalId, addressId)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );

    }
}