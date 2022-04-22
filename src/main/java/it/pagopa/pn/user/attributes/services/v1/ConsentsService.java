package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.mapper.v1.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.mapper.v1.ConsentEntityConsentDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.IConsentDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
public class ConsentsService {

    private IConsentDao consentDao;
    private ConsentEntityConsentDtoMapper consentEntityConsentDtoMapper;
    private ConsentActionDtoToConsentEntityMapper dtosToConsentEntityMapper;

    public ConsentsService(IConsentDao consentDao,
                           ConsentEntityConsentDtoMapper consentEntityConsentDtoMapper,
                           ConsentActionDtoToConsentEntityMapper consentActionDtoToConsentEntityMapper) {
        this.consentDao = consentDao;
        this.consentEntityConsentDtoMapper = consentEntityConsentDtoMapper;
        this.dtosToConsentEntityMapper = consentActionDtoToConsentEntityMapper;
    }

    public Mono<Void> consentAction(String recipientId, ConsentTypeDto consentType, Mono<ConsentActionDto> consentActionDto) {
        return consentActionDto
        .map(dto -> dtosToConsentEntityMapper.toEntity(recipientId, consentType, dto))
        .map(entity -> {
            Instant strDate = Instant.now();
            // qui vengono impostate entrambe le date.
            // Nel metodo ConsentDao->consentAction vengono sovrascritte in modo che:
            //   alla creazione del consenso created != null e lastModified == null
            //   alla modifica del consenso created non viene alterato e lastModified viene aggiornato

            entity.setCreated(strDate);
            entity.setLastModified(strDate);
            return entity;
        })
        .map(entity -> consentDao.consentAction(entity))
                .then();
    }

    public Mono<ConsentDto> getConsentByType(String recipientId, ConsentTypeDto consentType) {
        return consentDao.getConsentByType(recipientId, consentType)
                .map(ent -> consentEntityConsentDtoMapper.toDto(ent));
    }


    public Flux<ConsentDto> getConsents(String recipientId) {
        return consentDao.getConsents(recipientId)
                .map(ent -> consentEntityConsentDtoMapper.toDto(ent));
    }
}
