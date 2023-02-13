package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.CxTypeAuthFleetDto;
import it.pagopa.pn.user.attributes.mapper.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.middleware.db.ConsentDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ConsentsServiceTest {

    private final Duration d = Duration.ofMillis(3000);

    @InjectMocks
    private ConsentsService service;

    @Mock
    IConsentDao consentDao;

    @Mock
    ConsentActionDtoToConsentEntityMapper consentActionDtoToConsentEntityMapper;

    @Mock
    PnExternalRegistryClient pnExternalRegistryClient;

    @Test
    void consentAction_Accept() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto = ConsentTypeDto.DATAPRIVACY;

        ConsentActionDto consentActionDto = new ConsentActionDto();
        consentActionDto.setAction(ConsentActionDto.ActionEnum.ACCEPT);

        ConsentEntity ce = new ConsentEntity(recipientId, consentTypeDto.getValue(), null);
        ce.setAccepted(true);

        Mockito.when(consentActionDtoToConsentEntityMapper.toEntity(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ce);
        Mockito.when(consentDao.consentAction(Mockito.any())).thenReturn(Mono.just(new Object()));

    
        // WHEN
        Object result = service.consentAction(recipientId, CxTypeAuthFleetDto.PF, consentTypeDto, consentActionDto, null).block(d);

        //THEN
        assertNotNull( result );
    }


    @Test
    void consentAction_Decline() {

        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto = ConsentTypeDto.DATAPRIVACY;

        ConsentActionDto consentActionDto = new ConsentActionDto();
        consentActionDto.setAction(ConsentActionDto.ActionEnum.DECLINE);

        ConsentEntity ce = new ConsentEntity(recipientId, consentTypeDto.getValue(), null);
        ce.setAccepted(false);

        Mockito.when(consentActionDtoToConsentEntityMapper.toEntity(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ce);
        Mockito.when(consentDao.consentAction(Mockito.any())).thenReturn(Mono.just(new Object()));


        // WHEN
        Object result = service.consentAction(recipientId, CxTypeAuthFleetDto.PF, consentTypeDto, consentActionDto, null).block(d);

        //THEN
        assertNotNull( result );
    }

    @Test
    void getConsentByType() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(true);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1);


        Mockito.when(consentDao.getConsentByType(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(consentEntity));

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals( expected, result );
    }

    @Test
    void getConsentByType_differentVersion() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        String vers2 = "VERS2";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto PFexpected = new ConsentDto();
        PFexpected.setAccepted(false);
        PFexpected.setConsentType(dto);
        PFexpected.setConsentVersion(vers2);
        PFexpected.isFirstAccept(false);

        String PFrecipientId = CxTypeAuthFleetDto.PF + "-" + recipientId;

        ConsentEntity PFconsentEntity = new ConsentEntity(PFrecipientId, dto.getValue(), vers1);
        PFconsentEntity.setLastModified(Instant.now());
        PFconsentEntity.setAccepted(true);


        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers2));
        Mockito.when(consentDao.getConsentByType(Mockito.eq(PFrecipientId), Mockito.any(), Mockito.any())).thenReturn(Mono.just(PFconsentEntity));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals( PFexpected, result );
    }


    @Test
    void getConsentByType_differentCxType() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto PFexpected = new ConsentDto();
        PFexpected.setAccepted(false);
        PFexpected.setConsentType(dto);
        PFexpected.setConsentVersion(vers1);
        PFexpected.isFirstAccept(true);

        String PFrecipientId = CxTypeAuthFleetDto.PG + "-" + recipientId;

        ConsentEntity PFconsentEntity = new ConsentEntity(PFrecipientId, dto.getValue(), vers1);
        PFconsentEntity.setLastModified(Instant.now());
        PFconsentEntity.setAccepted(true);


        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PG.getValue())).thenReturn(Mono.just(vers1));
        Mockito.when(consentDao.getConsentByType(Mockito.eq(PFrecipientId), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PG, dto, "VERS1").block(d);

        //THEN
        assertEquals( PFexpected, result );
    }

    @Test
    void getConsentByType_NotFound() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto PFexpected = new ConsentDto();
        PFexpected.setAccepted(false);
        PFexpected.setConsentType(dto);
        PFexpected.setConsentVersion(vers1);
        PFexpected.isFirstAccept(true);

        Mockito.when(consentDao.getConsentByType(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));


        // WHEN
        Mono<ConsentDto> mono =  service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null);
        ConsentDto res = mono.block(d);

        //THEN
        assertNotNull(res);
        assertEquals(false, res.getAccepted());
    }


    @Test
    void getConsents() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto expected = new ConsentDto();
        List<ConsentEntity> list = new ArrayList<>();
        list.add(ConsentDaoTestIT.newConsent(true));
        list.add(ConsentDaoTestIT.newConsent(false));

        Mockito.when(consentDao.getConsents(Mockito.any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));


        // WHEN
        List<ConsentDto> result = service.getConsents(recipientId, CxTypeAuthFleetDto.PF).collectList().block(d);

        //THEN
        assertNotNull(result);
        assertEquals( list.size(), result.size() );
    }

    @Test
    void getConsents_NotFound() {
        //GIVEN
        String recipientId = "recipientid";

        Mockito.when(consentDao.getConsents(Mockito.any())).thenReturn(Flux.empty());


        // WHEN
        Mono<List<ConsentDto>> mono = service.getConsents(recipientId, CxTypeAuthFleetDto.PF).collectList();
        List<ConsentDto> res = mono.block(d);

        //THEN
        assertNotNull(res);
        assertEquals(0, res.size());
    }
}