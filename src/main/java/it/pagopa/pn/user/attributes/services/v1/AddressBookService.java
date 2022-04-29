package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.UserAddressesDto;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityListToUserAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.AddressBookDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AddressBookService {
    // Nel branch feature/PN-1283 il controllo del codice di verifica tramite external channell non viene fatto.
    // Temporaneamente il controllo viene fatto confrontando il codice immesso con una stringa costante
    public static final String VERIFICATION_CODE_OK = "12345";
    public static final String VERIFICATION_CODE_MISMATCH = "Verification code mismatch";

    private AddressBookDao dao;
    private AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto;
    private AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto;
    private AddressBookEntityListToUserAddressDtoMapper addressBookEntityListToDto;

    public AddressBookService(AddressBookDao dao,
                              AddressBookEntityToCourtesyDigitalAddressDtoMapper addressBookEntityToDto,
                              AddressBookEntityToLegalDigitalAddressDtoMapper legalDigitalAddressToDto,
                              AddressBookEntityListToUserAddressDtoMapper addressBookEntityListToDto) {
        this.dao = dao;
        this.addressBookEntityToDto = addressBookEntityToDto;
        this.legalDigitalAddressToDto = legalDigitalAddressToDto;
        this.addressBookEntityListToDto = addressBookEntityListToDto;
    }


    public Mono<Boolean> saveAddressBook(String recipientId, String senderId, boolean isLegal,  String channelType, Mono<AddressVerificationDto> addressVerificationDto) {
        return addressVerificationDto
                .flatMap(r -> dao.saveAddressBookEx(recipientId, senderId, isLegal, channelType, r.getValue(), r.getVerificationCode()));
    }

    public Mono<Object> deleteAddressBook(String recipientId, String senderId, boolean isLegal,  String channelType) {
        return dao.deleteAddressBook(recipientId, senderId, isLegal, channelType);
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressBySender(String recipientId, String senderId) {
        return dao.getCourtesyAddressBySender(recipientId, senderId)
                .map(ent -> addressBookEntityToDto.toDto(ent));
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getCourtesyAddressByRecipient(recipientId)
                .map(ent -> addressBookEntityToDto.toDto(ent));
    }

    public Flux<LegalDigitalAddressDto> getLegalAddressBySender(String recipientId, String senderId) {
        return dao.getLegalAddressBySender(recipientId, senderId)
                .map(ent -> legalDigitalAddressToDto.toDto(ent));

    }

    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return dao.getLegalAddressByRecipient(recipientId)
                .map(ent -> legalDigitalAddressToDto.toDto(ent));

    }

    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        return dao.getAddressesByRecipient(recipientId).collectList()
                .map(addressBookEntities -> addressBookEntityListToDto.toDto(addressBookEntities));
    }

}
