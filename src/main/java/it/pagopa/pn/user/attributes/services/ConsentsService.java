package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.mapper.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.mapper.ConsentEntityConsentDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ConsentsService {
    private final IConsentDao consentDao;
    private final ConsentEntityConsentDtoMapper consentEntityConsentDtoMapper;
    private final ConsentActionDtoToConsentEntityMapper dtosToConsentEntityMapper;

    public ConsentsService(IConsentDao consentDao,
                           ConsentEntityConsentDtoMapper consentEntityConsentDtoMapper,
                           ConsentActionDtoToConsentEntityMapper consentActionDtoToConsentEntityMapper) {
        this.consentDao = consentDao;
        this.consentEntityConsentDtoMapper = consentEntityConsentDtoMapper;
        this.dtosToConsentEntityMapper = consentActionDtoToConsentEntityMapper;
    }

    /**
     * Salva un nuovo consenso
     *
     * @param recipientId id utente
     * @param consentType tipo consenso
     * @param consentActionDto azione consenso
     * @return nd
     */
    public Mono<Object> consentAction(String recipientId, ConsentTypeDto consentType, ConsentActionDto consentActionDto, String version) {
        ConsentEntity consentEntity = dtosToConsentEntityMapper.toEntity(recipientId, consentType, consentActionDto, version);
        return consentDao.consentAction(consentEntity);
    }

    /**
     * Ritorna il consenso per tipologia
     *
     * @param recipientId id utente
     * @param consentType tipologia
     * @return il consenso
     */
    public Mono<ConsentDto> getConsentByType(String recipientId, ConsentTypeDto consentType, String version) {
        return consentDao.getConsentByType(recipientId, consentType.getValue(), version)
                .map(consentEntityConsentDtoMapper::toDto)
                .defaultIfEmpty(ConsentDto.builder()
                        .consentType(consentType)
                        .consentVersion(ConsentEntity.DEFAULT_VERSION)
                        .accepted(false)
                        .recipientId(recipientId)
                        .build());

    }

    /**
     * Ritorna i consensi per l'utente
     *
     * @param recipientId id utente
     * @return lista consensi
     */
    public Flux<ConsentDto> getConsents(String recipientId) {
        return consentDao.getConsents(recipientId)
                .map(consentEntityConsentDtoMapper::toDto);
    }
}
