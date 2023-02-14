package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import it.pagopa.pn.user.attributes.mapper.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.middleware.db.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ConsentsService {
    private final IConsentDao consentDao;
    private final ConsentActionDtoToConsentEntityMapper dtosToConsentEntityMapper;
    private final PnExternalRegistryClient pnExternalRegistryClient;

    public ConsentsService(IConsentDao consentDao,
                           ConsentActionDtoToConsentEntityMapper consentActionDtoToConsentEntityMapper, PnExternalRegistryClient pnExternalRegistryClient) {
        this.consentDao = consentDao;
        this.dtosToConsentEntityMapper = consentActionDtoToConsentEntityMapper;
        this.pnExternalRegistryClient = pnExternalRegistryClient;
    }

    /**
     * Salva un nuovo consenso
     *
     * @param xPagopaPnUid id utente
     * @param consentType tipo consenso
     * @param consentActionDto azione consenso
     * @return nd
     */
    public Mono<Object> consentAction(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, ConsentActionDto consentActionDto, String version) {
        ConsentEntity consentEntity = dtosToConsentEntityMapper.toEntity(computeRecipientIdWithCxType(xPagopaPnUid, xPagopaPnCxType), consentType, consentActionDto, version);
        return consentDao.consentAction(consentEntity);
    }

    /**
     * Ritorna il consenso per tipologia
     *
     * @param xPagopaPnUid id utente
     * @param consentType tipologia
     * @return il consenso
     */
    public Mono<ConsentDto> getConsentByType(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType) {
        String uidWithCxType = computeRecipientIdWithCxType(xPagopaPnUid, xPagopaPnCxType);
        return pnExternalRegistryClient.findPrivacyNoticeVersion(consentType.getValue(), xPagopaPnCxType.getValue())
                .zipWhen(version -> consentDao.getConsentByType(uidWithCxType, consentType.getValue(), version)
                                                .switchIfEmpty(consentDao.getConsents(uidWithCxType).filter(x -> consentType.getValue().equals(x.getConsentType())).take(1).next())
                                                .defaultIfEmpty(new ConsentEntity(uidWithCxType, consentType.getValue(), ConsentEntity.NONEACCEPTED_VERSION)),
                        (noticeversion, entity) -> ConsentDto.builder()
                                .consentVersion(noticeversion)
                                .consentType(consentType)
                                .recipientId(uidWithCxType)
                                .isFirstAccept(entity.getConsentVersion().equals(ConsentEntity.NONEACCEPTED_VERSION))
                                .accepted(entity.isAccepted() && entity.getConsentVersion().equals(noticeversion))
                                .build()
                );

    }


    /**
     * Ritorna i consensi per l'utente
     *
     * @param xPagopaPnUid id utente
     * @return lista consensi
     */
    public Flux<ConsentDto> getConsents(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType) {
        String uidWithCxType = computeRecipientIdWithCxType(xPagopaPnUid, xPagopaPnCxType);
        return consentDao.getConsents(uidWithCxType)
                .flatMap(x -> pnExternalRegistryClient.findPrivacyNoticeVersion(x.getConsentType(), xPagopaPnCxType.getValue())
                                .map(y -> ConsentDto.builder()
                                        .consentVersion(y)
                                        .recipientId(uidWithCxType)
                                        .consentType(ConsentTypeDto.fromValue(x.getConsentType()))
                                        .isFirstAccept(x.getConsentVersion().equals(ConsentEntity.NONEACCEPTED_VERSION))
                                        .accepted(x.getConsentVersion().equals(y))
                                        .build()));
    }

    private String computeRecipientIdWithCxType(String recipientId, CxTypeAuthFleetDto xPagopaPnCxType){
        return xPagopaPnCxType.getValue() + "-" + recipientId;
    }
}
