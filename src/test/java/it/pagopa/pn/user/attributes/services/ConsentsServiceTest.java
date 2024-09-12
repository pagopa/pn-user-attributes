package it.pagopa.pn.user.attributes.services;

import it.pagopa.pn.user.attributes.exceptions.PnForbiddenException;
import it.pagopa.pn.user.attributes.mapper.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.middleware.db.ConsentDaoTestIT;
import it.pagopa.pn.user.attributes.middleware.db.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.wsclient.PnExternalRegistryClient;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentActionDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxTypeAuthFleetDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

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

        Mockito.when(consentActionDtoToConsentEntityMapper.toEntity(Mockito.anyString(), any(), any(), any())).thenReturn(ce);
        Mockito.when(consentDao.consentAction(any())).thenReturn(Mono.just(new Object()));

    
        // WHEN
        Object result = service.consentAction(recipientId, CxTypeAuthFleetDto.PF, consentTypeDto, consentActionDto, null).block(d);

        //THEN
        assertNotNull(result);
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

        Mockito.when(consentActionDtoToConsentEntityMapper.toEntity(Mockito.anyString(), any(), any(), any())).thenReturn(ce);
        Mockito.when(consentDao.consentAction(any())).thenReturn(Mono.just(new Object()));


        // WHEN
        Object result = service.consentAction(recipientId, CxTypeAuthFleetDto.PF, consentTypeDto, consentActionDto, null).block(d);

        //THEN
        assertNotNull(result);
    }


    @Test
    void getConsentByType_version() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(true);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(true);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.just(consentEntity));
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.empty());

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, vers1).block(d);

        //THEN
        assertEquals(expected, result);
    }


    @Test
    void getConsentByType_version_notaccepted() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(false);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(false);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.just(consentEntity));
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.empty());

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, vers1).block(d);

        //THEN
        assertEquals(expected, result);
    }


    @Test
    void getConsentByType_version_notfound() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(false);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1 + "OLD");
        consentEntity.setAccepted(true);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.empty());
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.fromIterable(List.of(consentEntity)));

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, vers1).block(d);

        //THEN
        assertEquals(expected, result);
    }

    @Test
    void getConsentByType() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(true);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(true);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.just(consentEntity));
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.empty());

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals(expected, result);
    }


    @Test
    void getConsentByType_notaccepted() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(false);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(false);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.just(consentEntity));
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.empty());

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals(expected, result);
    }

    @Test
    void getConsentByType_isFirst() {
        //GIVEN
        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(false);
        expected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(true);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1 + "_NO");
        consentEntity.setAccepted(true);


        Mockito.when(consentDao.getConsentByType(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId, dto.getValue(), vers1)).thenReturn(Mono.empty());
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.empty());

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals(expected, result);
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
        PFexpected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        PFexpected.setConsentVersion(vers2);
        PFexpected.isFirstAccept(false);

        String PFrecipientId = CxTypeAuthFleetDto.PF + "-" + recipientId;

        ConsentEntity PFconsentEntity = new ConsentEntity(PFrecipientId, dto.getValue(), vers1);
        PFconsentEntity.setLastModified(Instant.now());
        PFconsentEntity.setAccepted(true);


        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers2));
        Mockito.when(consentDao.getConsentByType(Mockito.eq(PFrecipientId), any(), any())).thenReturn(Mono.just(PFconsentEntity));
        Mockito.when(consentDao.getConsents(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId)).thenReturn(Flux.fromIterable(List.of(PFconsentEntity)));

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null).block(d);

        //THEN
        assertEquals(PFexpected, result);
    }


    @Test
    void getConsentByType_differentCxType() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto PGexpected = new ConsentDto();
        PGexpected.setAccepted(false);
        PGexpected.recipientId(CxTypeAuthFleetDto.PG.getValue() + "-" + recipientId);
        PGexpected.setConsentType(dto);
        PGexpected.setConsentVersion(vers1);
        PGexpected.isFirstAccept(true);

        String PGrecipientId = CxTypeAuthFleetDto.PG + "-" + recipientId;

        ConsentEntity PFconsentEntity = new ConsentEntity(PGrecipientId, dto.getValue(), vers1);
        PFconsentEntity.setLastModified(Instant.now());
        PFconsentEntity.setAccepted(true);


        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PG.getValue())).thenReturn(Mono.just(vers1));
        Mockito.when(consentDao.getConsentByType(Mockito.eq(PGrecipientId), any(), any())).thenReturn(Mono.empty());
        Mockito.when(consentDao.getConsents(PGrecipientId)).thenReturn(Flux.empty());

        // WHEN
        ConsentDto result = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PG, dto, null).block(d);

        //THEN
        assertEquals(PGexpected, result);
    }

    @Test
    void getConsentByType_NotFound() {
        //GIVEN
        String recipientId = "recipientid";
        String vers1 = "VERS1";
        ConsentTypeDto dto = ConsentTypeDto.TOS;
        ConsentDto PFexpected = new ConsentDto();
        PFexpected.setAccepted(false);
        PFexpected.recipientId(CxTypeAuthFleetDto.PF.getValue() + "-" + recipientId);
        PFexpected.setConsentType(dto);
        PFexpected.setConsentVersion(vers1);
        PFexpected.isFirstAccept(true);

        Mockito.when(consentDao.getConsentByType(any(), any(), any())).thenReturn(Mono.empty());
        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));
        Mockito.when(consentDao.getConsents(any())).thenReturn(Flux.empty());

        // WHEN
        Mono<ConsentDto> mono = service.getConsentByType(recipientId, CxTypeAuthFleetDto.PF, dto, null);
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

        Mockito.when(consentDao.getConsents(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue())).thenReturn(Mono.just(vers1));


        // WHEN
        List<ConsentDto> result = service.getConsents(recipientId, CxTypeAuthFleetDto.PF).collectList().block(d);

        //THEN
        assertNotNull(result);
        assertEquals(list.size(), result.size());
    }

    @Test
    void getConsents_NotFound() {
        //GIVEN
        String recipientId = "recipientid";

        Mockito.when(consentDao.getConsents(any())).thenReturn(Flux.empty());


        // WHEN
        Mono<List<ConsentDto>> mono = service.getConsents(recipientId, CxTypeAuthFleetDto.PF).collectList();
        List<ConsentDto> res = mono.block(d);

        //THEN
        assertNotNull(res);
        assertEquals(0, res.size());
    }

    @Test
    void getPgConsentByType() {
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PG;

        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS_DEST_B2B;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(true);
        expected.recipientId(CxTypeAuthFleetDto.PG.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PG.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(true);

        Mockito.when(consentDao.getConsentByType(any(), any(), any()))
                .thenReturn(Mono.just(consentEntity));

        Mockito.when(consentDao.getConsents(any()))
                .thenReturn(Flux.just(consentEntity));

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PG.getValue()))
                .thenReturn(Mono.just(vers1));

        Mono<ConsentDto> result = service.getPgConsentByType(recipientId, xPagopaPnCxType,
                dto, vers1);

        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void getPgConsentByType_ErrorTest() {
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PF;

        String recipientId = "recipientid";
        ConsentTypeDto dto = ConsentTypeDto.TOS_DEST_B2B;
        String vers1 = "VERS1";
        ConsentDto expected = new ConsentDto();
        expected.setAccepted(true);
        expected.recipientId(CxTypeAuthFleetDto.PG.getValue() + "-" + recipientId);
        expected.setConsentType(dto);
        expected.setConsentVersion(vers1);
        expected.isFirstAccept(false);

        ConsentEntity consentEntity = new ConsentEntity(CxTypeAuthFleetDto.PG.getValue() + "-" + recipientId, dto.getValue(), vers1);
        consentEntity.setAccepted(true);

        Mockito.when(pnExternalRegistryClient.findPrivacyNoticeVersion(dto.getValue(), CxTypeAuthFleetDto.PF.getValue()))
                .thenReturn(Mono.just(vers1));

        Mono<ConsentDto> result = service.getPgConsentByType(recipientId, xPagopaPnCxType,
                dto, vers1);

        StepVerifier.create(result)
                .expectError();
    }

    @Test
    void testSetPgConsentAction_Admin() {

        String xPagopaPnCxId = "testId";
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PG;
        String xPagopaPnCxRole = "ADMIN";
        ConsentTypeDto consentType = ConsentTypeDto.TOS_DEST_B2B;
        String version = "v1";
        ConsentActionDto consentActionDto = new ConsentActionDto();
        List<String> xPagopaPnCxGroups = Collections.emptyList();

        ConsentEntity consentEntity = new ConsentEntity("recipientId", "consentType", "version");
        Mockito.when(consentActionDtoToConsentEntityMapper.toEntity(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(consentEntity);
        Mockito.when(consentDao.consentAction(consentEntity)).thenReturn(Mono.empty());

        Mono<Void> result = service.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, consentActionDto, xPagopaPnCxGroups);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testSetPgConsentAction_Not_PG() {

        String xPagopaPnCxId = "testId";
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PF;
        String xPagopaPnCxRole = "USER";
        ConsentTypeDto consentType = ConsentTypeDto.TOS_DEST_B2B;
        String version = "v1";
        ConsentActionDto consentActionDto = new ConsentActionDto();
        List<String> xPagopaPnCxGroups = List.of("group1");

        Mono<Void> result = service.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, consentActionDto, xPagopaPnCxGroups);

        StepVerifier.create(result)
                .expectError(PnForbiddenException.class)
                .verify();
    }

    @Test
    void testSetPgConsentAction_Not_Admin() {

        String xPagopaPnCxId = "testId";
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PG;
        String xPagopaPnCxRole = "USER";
        ConsentTypeDto consentType = ConsentTypeDto.TOS_DEST_B2B;
        String version = "v1";
        ConsentActionDto consentActionDto = new ConsentActionDto();
        List<String> xPagopaPnCxGroups = List.of("group1");

        Mono<Void> result = service.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, consentActionDto, xPagopaPnCxGroups);

        StepVerifier.create(result)
                .expectError(PnForbiddenException.class)
                .verify();
    }

    @Test
    void testSetPgConsentAction_AdminGroupNotEmpty() {

        String xPagopaPnCxId = "testId";
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PG;
        String xPagopaPnCxRole = "ADMIN";
        ConsentTypeDto consentType = ConsentTypeDto.TOS_DEST_B2B;
        String version = "v1";
        ConsentActionDto consentActionDto = new ConsentActionDto();
        List<String> xPagopaPnCxGroups = List.of("group1");

        Mono<Void> result = service.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, consentActionDto, xPagopaPnCxGroups);

        StepVerifier.create(result)
                .expectError(PnForbiddenException.class)
                .verify();
    }

    @Test
    void testSetPgConsentAction_ContentTypeNotTOS_DEST_B2B() {

        String xPagopaPnCxId = "testId";
        CxTypeAuthFleetDto xPagopaPnCxType = CxTypeAuthFleetDto.PG;
        String xPagopaPnCxRole = "ADMIN";
        ConsentTypeDto consentType = ConsentTypeDto.TOS;
        String version = "v1";
        ConsentActionDto consentActionDto = new ConsentActionDto();
        List<String> xPagopaPnCxGroups = Collections.emptyList();

        Mono<Void> result = service.setPgConsentAction(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, consentType, version, consentActionDto, xPagopaPnCxGroups);

        StepVerifier.create(result)
                .expectError(PnForbiddenException.class)
                .verify();
    }
}