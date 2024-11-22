package it.pagopa.pn.user.attributes.middleware.wsclient;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.datavault.v1.dto.RecipientAddressesDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalAddressTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.LegalChannelTypeDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static it.pagopa.pn.user.attributes.services.utils.VerificationCodeUtils.composeVcAddressId;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnDataVaultClient {
    
    private final AddressBookApi addressBookApi;
    private final RecipientsApi recipientsApi;

    public PnDataVaultClient(AddressBookApi addressBookApi, RecipientsApi recipientsApi) {
        this.addressBookApi = addressBookApi;
        this.recipientsApi = recipientsApi;
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
    public Mono<Void> updateRecipientAddressByInternalId(String internalId, String addressId, String realaddress, BigDecimal ttl)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Updating recipient address");
        log.debug("updateRecipientAddressByInternalId internalId={} addressId={}", internalId, addressId);
        AddressDtoDto dto = new AddressDtoDto();
        dto.setValue(realaddress);
        return addressBookApi.updateRecipientAddressByInternalId (internalId, addressId, ttl, dto);
            
    }

    public Mono<Void> updateRecipientAddressByInternalId(String internalId, String addressId, String realaddress)
    {
        return updateRecipientAddressByInternalId(internalId, addressId, realaddress, null);
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
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Retrieving recipient addresses");
        log.debug("getRecipientAddressesByInternalId internalId:{}", internalId);
        return addressBookApi.getRecipientAddressesByInternalId (internalId);
    }

    /**
     * Recupera un indirizzo in fase di verifica per un utente
     *
     * @param internalId uid dell'utente
     * @param hashedAddress hash dell'indirizzo
     * @return l'indirizzo
     */
    public Mono<AddressDtoDto> getVerificationCodeAddressByInternalId(String internalId, String hashedAddress) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Retrieving verification code address");
        log.debug("getVerificationCodeAddressByInternalId internalId:{}, hashedAddress:{}", internalId, hashedAddress);
        return addressBookApi.getRecipientAddressesByInternalId(internalId)
                .map(RecipientAddressesDtoDto::getAddresses)
                .mapNotNull(addresses -> addresses.get(composeVcAddressId(hashedAddress)));
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
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Deleting recipient address");
        log.debug("deleteRecipientAddressByInternalId internalId={} addressId={}", internalId, addressId);
        return addressBookApi.deleteRecipientAddressByInternalId  (internalId, addressId);

    }


    /**
     * Ritorna una lista di nominativi in base alla lista di iuid passati
     *
     * @param internalIds lista di iuid
     * @return lista di nominativi
     */
    public Flux<BaseRecipientDtoDto> getRecipientDenominationByInternalId(List<String> internalIds)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Opaque Ids Resolution");
        log.debug("getRecipientDenominationByInternalId internalIds={} ", internalIds);
        return recipientsApi.getRecipientDenominationByInternalId(internalIds);

    }
}
