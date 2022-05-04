package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.LegalDigitalAddressDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.UserAddressesDto;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityListToUserAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToCourtesyDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.mapper.v1.AddressBookEntityToLegalDigitalAddressDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.AddressBookDao;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.VerificationCodeEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        String legal = isLegal?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue();
        return addressVerificationDto
                .flatMap(r -> {
                    String hashed = DigestUtils.sha256Hex(r.getValue());
                    return dao.getVerifiedAddress(recipientId, hashed)
                            .map(v -> true); //FAKE
                            /*.switchIfEmpty(() -> {
                                if (StringUtils.hasText(r.getVerificationCode())) {
                                    VerificationCodeEntity verificationCodeEntity = new VerificationCodeEntity(recipientId, hashed, channelType);
                                    verificationCodeEntity.setVerificationCode(getNewVerificationCode());
                                    verificationCodeEntity.setCreated(Instant.now());
                                    verificationCodeEntity.setLastModified(verificationCodeEntity.getCreated());
                                    verificationCodeEntity.setTtl(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toEpochSecond());
                                    return dao.saveVerificationCode(verificationCodeEntity);
                                } else {
                                    return Mono.empty();
                                }
                            });*/
                });
    }

    public Mono<Object> deleteAddressBook(String recipientId, String senderId, boolean isLegal,  String channelType) {
        String legal = isLegal?LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue():CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue();
        return dao.deleteAddressBook(recipientId, senderId, legal, channelType);
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressBySender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .map(ent -> addressBookEntityToDto.toDto(ent));
    }

    public Flux<CourtesyDigitalAddressDto> getCourtesyAddressByRecipient(String recipientId) {
        return dao.getAddresses(recipientId, null, CourtesyDigitalAddressDto.AddressTypeEnum.COURTESY.getValue())
                .map(ent -> addressBookEntityToDto.toDto(ent));
    }

    public Flux<LegalDigitalAddressDto> getLegalAddressBySender(String recipientId, String senderId) {
        return dao.getAddresses(recipientId, senderId, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .map(ent -> legalDigitalAddressToDto.toDto(ent));

    }

    public Flux<LegalDigitalAddressDto> getLegalAddressByRecipient(String recipientId) {
        return dao.getAddresses(recipientId, null, LegalDigitalAddressDto.AddressTypeEnum.LEGAL.getValue())
                .map(ent -> legalDigitalAddressToDto.toDto(ent));

    }

    public Mono<UserAddressesDto> getAddressesByRecipient(String recipientId) {
        return dao.getAllAddressesByRecipient(recipientId).collectList()
                .map(addressBookEntities -> addressBookEntityListToDto.toDto(addressBookEntities));
    }


    private String getNewVerificationCode() {
        log.debug("generated a new verificationCode: {}", AddressBookService.VERIFICATION_CODE_OK);
        return AddressBookService.VERIFICATION_CODE_OK;
    }
}
