package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.generated.openapi.server.user.consents.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
class ConsentDaoTestIT {
    private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String RECIPIENTID_NOT_FOUND = "not-existent-recid";

    private static final String CONSENTTYPE = "TOS";

    private IConsentDao consentDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @BeforeEach
    void setUp(@Value(ConsentDao.DYNAMODB_TABLE_NAME) String table) {
        consentDao = new ConsentDao(dynamoDbEnhancedAsyncClient, table);
    }

    @Test
    void consentAction() {
        String pk = ConsentEntity.getPk(RECIPIENTID);
        ConsentEntity consentEntity = ConsentEntity
                                      .builder()
                                      .recipientId(pk)
                                      .consentType(CONSENTTYPE)
                                      .accepted(true)
                                      .build();

        ConsentTypeDto ctDto = ConsentTypeDto.TOS;

        try {
            Object ret = consentDao.consentAction(consentEntity).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            fail(e);
        }

        try {
            ConsentEntity ret = consentDao.getConsentByType(RECIPIENTID, ctDto).block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertNotNull(ret.getCreated());
            assertEquals(consentEntity.getConsentType(),ret.getConsentType());
            assertEquals(consentEntity.getRecipientId(),ret.getRecipientId());
            assertEquals(consentEntity.isAccepted(),ret.isAccepted());
        } catch (Exception e) {
            fail(e);
        }

    }


    @Test
    void getConsents() {
        try {
            List<ConsentEntity> ret = consentDao.getConsents(RECIPIENTID).collectList().block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertTrue(ret.size() > 0);
        } catch (Exception e) {
            fail(e);
        }

        // test di un recipientId non esistente
        try {
            List<ConsentEntity> ret = consentDao.getConsents(RECIPIENTID_NOT_FOUND).collectList().block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertEquals(0, ret.size());
        } catch (Exception e) {
            fail(e);
        }
    }
}
