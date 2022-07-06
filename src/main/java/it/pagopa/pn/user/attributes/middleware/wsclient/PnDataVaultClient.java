package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.ApiClient;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.middleware.wsclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
@Slf4j
public class PnDataVaultClient extends BaseClient {
    
    private AddressBookApi addressBookApi;
    private RecipientsApi recipientsApi;
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnDataVaultClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        this.addressBookApi = new AddressBookApi(apiClient);

        apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()).build());
        apiClient.setBasePath(pnUserattributesConfig.getClientDatavaultBasepath());

        this.recipientsApi = new RecipientsApi(apiClient);
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
        log.info("updateRecipientAddressByInternalId internalId:{} addressId:{}", internalId, addressId);
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
        log.info("getRecipientAddressesByInternalId internalId:{}", internalId);
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
        log.info("deleteRecipientAddressByInternalId internalId={} addressId={}", internalId, addressId);
        return addressBookApi.deleteRecipientAddressByInternalId  (internalId, addressId)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );

    }


    /**
     * Ritorna una lista di nominativi in base alla lista di iuid passati
     *
     * @param internalIds lista di iuid
     * @return lista di nominativi
     */
    public Flux<BaseRecipientDtoDto> getRecipientDenominationByInternalId(List<String> internalIds)
    {
        return recipientsApi.getRecipientDenominationByInternalId(internalIds)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );

    }
}
