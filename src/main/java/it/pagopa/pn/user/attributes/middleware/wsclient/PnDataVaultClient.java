package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
public class PnDataVaultClient {
    
    private AddressBookApi addressBookApi;
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnDataVaultClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        WebClient webClient = ApiClient.buildWebClientBuilder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        ApiClient newApiClient = new ApiClient(webClient);
        newApiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());
        this.addressBookApi = new AddressBookApi(newApiClient);
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
