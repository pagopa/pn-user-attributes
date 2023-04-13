package it.pagopa.pn.user.attributes.services.utils;

import it.pagopa.pn.user.attributes.middleware.db.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerifiedAddressUtils {

    private final AddressBookDao dao;


    /**
     * Salva in dynamodb l'id offuscato
     *
     * @param addressBook addressBook da salvare, COMPLETO di hashedaddress impostato
     * @return nd
     */
    public Mono<Void> saveInDynamodb(AddressBookEntity addressBook){
        log.info("saving address in db uid:{} hashedaddress:{} channel:{} legal:{}", addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType(), addressBook.getAddressType());

        VerifiedAddressEntity verifiedAddressEntity = new VerifiedAddressEntity(addressBook.getRecipientId(), addressBook.getAddresshash(), addressBook.getChannelType());

        return this.dao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddressEntity);
    }

}