package it.pagopa.pn.user.attributes.services.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentDto;
import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.mapper.v1.ConsentActionDtoToConsentEntityMapper;
import it.pagopa.pn.user.attributes.mapper.v1.ConsentEntityConsentDtoMapper;
import it.pagopa.pn.user.attributes.middleware.db.v1.ConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.v1.IConsentDao;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ConsentsServiceTest {
    private IConsentDao consentDao;
    private ConsentsService consentsService;
    private ConsentEntityConsentDtoMapper consentEntityConsentDtoMapper;
    private ConsentActionDtoToConsentEntityMapper consentActionDtoToConsentEntityMapper;

    @BeforeEach
    void setUp() {
        consentEntityConsentDtoMapper = new ConsentEntityConsentDtoMapper();
        consentActionDtoToConsentEntityMapper = new ConsentActionDtoToConsentEntityMapper();
        consentDao = new ConsentsServiceTest.MockConsentDao();
        consentsService = new ConsentsService(consentDao, consentEntityConsentDtoMapper, consentActionDtoToConsentEntityMapper);
    }

    @Test
    void consentAction() {

        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto = ConsentTypeDto.DATAPRIVACY;

        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(ConsentEntity.getPk(recipientId));
        ce.setConsentType(consentTypeDto.getValue());
        ce.setAccepted(false);

        ConsentDto consentDto = consentEntityConsentDtoMapper.toDto(ce);

        Mono<Object> ret = consentDao.consentAction(ce);
        assertEquals(Mono.empty(), ret);

        ConsentDto consentDtoRead = new ConsentDto();
        consentsService.getConsentByType(recipientId, consentTypeDto)
                .map(dto -> {
                    consentDtoRead.setRecipientId(dto.getRecipientId());
                    consentDtoRead.setConsentType(dto.getConsentType());
                    consentDtoRead.setAccepted(dto.getAccepted());
                    return dto;
                }).block(Duration.ofMillis(3000));

        assertEquals( consentDto, consentDtoRead );
    }

    @Test
    void getConsentByType() {
        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto = ConsentTypeDto.TOS;

        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(ConsentEntity.getPk(recipientId));
        ce.setConsentType(consentTypeDto.getValue());
        ce.setAccepted(true);

        ConsentDto consentDto = consentEntityConsentDtoMapper.toDto(ce);

        consentDao.consentAction(ce);

        ConsentDto consentDtoRead = new ConsentDto();
        consentsService.getConsentByType(recipientId, consentTypeDto)
                .map(dto -> {
                    consentDtoRead.setRecipientId(dto.getRecipientId());
                    consentDtoRead.setConsentType(dto.getConsentType());
                    consentDtoRead.setAccepted(dto.getAccepted());
                    return dto;
                }).block(Duration.ofMillis(3000));

        assertEquals( consentDto, consentDtoRead );
    }

    @Test
    void getConsentByType_NotFound() {
        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto = ConsentTypeDto.TOS;

        ConsentDto consentDto = new ConsentDto();
        consentDto.setRecipientId(recipientId);
        consentDto.setConsentType(consentTypeDto);
        consentDto.setAccepted(true);

        ConsentEntity ce = new ConsentEntity();
        ce.setRecipientId(recipientId);
        ce.setConsentType(consentTypeDto.getValue());
        ce.setAccepted(true);

        ConsentDto consentDtoRead = new ConsentDto();
        consentsService.getConsentByType(recipientId, consentTypeDto).map(consentDto1 -> {
            consentDtoRead.setRecipientId(consentDto1.getRecipientId());
            consentDtoRead.setConsentType(consentDto1.getConsentType());
            consentDtoRead.setAccepted(consentDto1.getAccepted());
            return consentDto1;
        });

        assertEquals( new ConsentDto(), consentDtoRead );
    }

    @Test
    void getConsents_NotFound() {
        String recipientId_inserted = "recipientid_1";
        String recipientId_read = "recipientid_2";
        ConsentTypeDto consentTypeDto1 = ConsentTypeDto.TOS;

        ConsentEntity ce1 = new ConsentEntity();
        ce1.setRecipientId(ConsentEntity.getPk(recipientId_inserted));
        ce1.setConsentType(consentTypeDto1.getValue());
        ce1.setAccepted(true);

        ConsentDto consentDto1 = consentEntityConsentDtoMapper.toDto(ce1);

        consentDao.consentAction(ce1);

        List<ConsentDto> listDto = new ArrayList<>();

        List<ConsentDto> listDtoRead = new ArrayList<>();
        consentsService.getConsents(recipientId_read)
                .map(dto -> {
                    ConsentDto d = new ConsentDto();
                    d.setRecipientId(dto.getRecipientId());
                    d.setConsentType(dto.getConsentType());
                    d.setAccepted(dto.getAccepted());
                    listDtoRead.add(d);
                    return dto;
                }).collectList().block(Duration.ofMillis(3000));

        assertEquals( listDto, listDtoRead );
    }

    @Test
    void getConsents() {
        String recipientId = "recipientid";
        ConsentTypeDto consentTypeDto1 = ConsentTypeDto.TOS;
        ConsentTypeDto consentTypeDto2 = ConsentTypeDto.DATAPRIVACY;

        ConsentEntity ce1 = new ConsentEntity();
        ce1.setRecipientId(ConsentEntity.getPk(recipientId));
        ce1.setConsentType(consentTypeDto1.getValue());
        ce1.setAccepted(true);

        ConsentDto consentDto1 = consentEntityConsentDtoMapper.toDto(ce1);

        consentDao.consentAction(ce1);

        ConsentEntity ce2 = new ConsentEntity();
        ce2.setRecipientId(ConsentEntity.getPk(recipientId));
        ce2.setConsentType(consentTypeDto2.getValue());
        ce2.setAccepted(false);

        ConsentDto consentDto2 = consentEntityConsentDtoMapper.toDto(ce2);

        consentDao.consentAction(ce2);

        List<ConsentDto> listDto = new ArrayList<>();
        listDto.add(consentDto1);
        listDto.add(consentDto2);

        List<ConsentDto> listDtoRead = new ArrayList<>();
        consentsService.getConsents(recipientId)
                .map(dto -> {
                    ConsentDto d = new ConsentDto();
                    d.setRecipientId(dto.getRecipientId());
                    d.setConsentType(dto.getConsentType());
                    d.setAccepted(dto.getAccepted());
                    listDtoRead.add(d);
                    return dto;
                }).collectList().block(Duration.ofMillis(3000));

        assertEquals( listDto, listDtoRead );
    }

    private static class MockConsentDao implements IConsentDao {

        private final Map<Key,ConsentEntity> store = new ConcurrentHashMap<>();

        public void put(ConsentEntity entity) {
            Key key = Key.builder()
                    .partitionValue(entity.getRecipientId())
                    .sortValue(entity.getConsentType())
                    .build();
            this.store.put(key, entity);
        }


        public ConsentEntity get(Key key) {
            return store.get(key);
        }

        @Override
        public Mono<Object> consentAction(ConsentEntity userAttributes) {
            if (userAttributes.getCreated() == null)
                userAttributes.setCreated(Instant.now());
            else
                userAttributes.setLastModified(Instant.now());

            put(userAttributes);
            return Mono.empty();
        }

        @Override
        public Mono<ConsentEntity> getConsentByType(String recipientId, ConsentTypeDto consentType) {
            Key key = Key.builder()
                    .partitionValue(ConsentEntity.getPk(recipientId))
                    .sortValue(consentType.getValue())
                    .build();

            ConsentEntity entity = get(key);
            if (entity == null)
                return Mono.empty();
            else
                return Mono.just(entity);
        }

        @Override
        public Flux<ConsentEntity> getConsents(String recipientId) {
            List<ConsentEntity> list = new ArrayList<>();

            for (var entry : store.entrySet()) {
                if (entry.getValue().getRecipientId().equals(ConsentEntity.getPk(recipientId)))
                    list.add(entry.getValue());
            }
            if (list.isEmpty())
                return Flux.empty();
            else {
                return Flux.just(list.toArray(new ConsentEntity[0]));
            }
        }
    }
}