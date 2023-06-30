package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.mapper.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.middleware.db.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
     * Ritorna il consenso per tipologia.
     * Il nuovo algoritmo prevede che:
     * - recupero sempre da ext-registry l'ultima versione disponibile
     *   - se version è NULL, cerco in DB se ho il consenso con la versione recuperata da ext-registry
     *   - se version NON è null, cerco in DB se ho il consenso con la versione richiesta.
     * - Poi, nel caso in cui non trovo proprio la versione, faccio una query per recuperare tutte le versioni per sapere se ce ne sono.
     * - Se la query per tutte le versioni non ne torna, genero un record fittizio, che sta a indicare che non ne ho accettata nessuna
     *
     * @param xPagopaPnUid id utente
     * @param consentType tipologia
     * @return il consenso
     */
    public Mono<ConsentDto> getConsentByType(String xPagopaPnUid, CxTypeAuthFleetDto xPagopaPnCxType, ConsentTypeDto consentType, String version) {
        String uidWithCxType = computeRecipientIdWithCxType(xPagopaPnUid, xPagopaPnCxType);
        return pnExternalRegistryClient.findPrivacyNoticeVersion(consentType.getValue(), xPagopaPnCxType.getValue())
                .zipWhen(lastConsentVersion -> consentDao.getConsentByType(uidWithCxType, consentType.getValue(), StringUtils.hasText(version)?version:lastConsentVersion)
                                                .switchIfEmpty(consentDao.getConsents(uidWithCxType).filter(x -> consentType.getValue().equals(x.getConsentType())).take(1).next())
                                                .defaultIfEmpty(new ConsentEntity(uidWithCxType, consentType.getValue(), ConsentEntity.NONEACCEPTED_VERSION)),
                        (lastConsentVersion, entity) -> ConsentDto.builder()
                                .consentVersion(StringUtils.hasText(version)?version:lastConsentVersion) // è sempre quella richiesta se passata, oppure l'ultima
                                .consentType(consentType)
                                .recipientId(uidWithCxType)
                                .isFirstAccept(entity.getConsentVersion().equals(ConsentEntity.NONEACCEPTED_VERSION))   // se l'entity letta è NONEACCEPTED, vuol dire che non ne ho trovate!
                                // qui è un pò più tricky: metto in AND con il fatto che sia accettata il fatto che ho trovato l'entity che cercavo.
                                // da notare che se non ho trovato entity (o se ne ho trovata una DIVERSA dalla versione che mi interessava o di DEFAULT), fallirà la seconda condizione
                                .accepted(entity.isAccepted() && entity.getConsentVersion().equals(StringUtils.hasText(version)?version:lastConsentVersion))
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
