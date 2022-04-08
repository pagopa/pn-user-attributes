package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.AddressVerificationDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.address.book.api.v1.dto.CourtesyChannelTypeDto;
import it.pagopa.pn.user.attributes.mapper.v1.AddressVerificationDtoToVerificationCodeEntityMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.VerificationCodeDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
public class CourtesyVerificationCodeService {

    private VerificationCodeDao dao;
    private AddressVerificationDtoToVerificationCodeEntityMapper dtoToEntityMapper;

    public CourtesyVerificationCodeService(VerificationCodeDao dao,
                                          AddressVerificationDtoToVerificationCodeEntityMapper dtoToEntityMapper) {
        this.dtoToEntityMapper = dtoToEntityMapper;
        this.dao = dao;
    }

    public Mono<Void> postRecipientCourtesyAddress(String recipientId, String senderId, CourtesyChannelTypeDto channelType, Mono<AddressVerificationDto> addressVerificationDto) {
        return addressVerificationDto
        .map(dto -> dtoToEntityMapper.toEntity(recipientId, channelType, dto))
        .map(entity -> {
            String strDate = Instant.now().toString();
            // qui vengono impostate entrambe le date.
            // Nel metodo VerificationCodeDao->saveVerificationCode vengono sovrascritte in modo che:
            //   alla creazione del verificationCode created != null e lastModified == null
            //   alla modifica del verificationCode created non viene alterato e lastModified viene aggiornato

            entity.setCreated(strDate);
            entity.setLastModified(strDate);
            return entity;
        })
        .map(entity -> dao.saveVerificationCode(entity))
                .then();
    }

}
