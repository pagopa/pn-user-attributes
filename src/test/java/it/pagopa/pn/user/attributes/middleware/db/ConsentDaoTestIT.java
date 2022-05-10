package it.pagopa.pn.user.attributes.middleware.db;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.generated.openapi.server.rest.api.v1.dto.ConsentTypeDto;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566"
})
@SpringBootTest
class ConsentDaoTestIT {


    @Autowired
    private ConsentDao consentDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    TestDao<ConsentEntity> testDao;

    @BeforeEach
    void setup(PnUserattributesConfig pnUserattributesConfig) {
        testDao = new TestDao(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), ConsentEntity.class);
    }

    @Test
    void consentAction() {
        //Given
        ConsentEntity consentToInsert = newConsent(false);

        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));

        //Then
        try {
            ConsentEntity elementFromDb = testDao.get(consentToInsert.getPk(), consentToInsert.getSk());

            Assertions.assertNotNull(elementFromDb);

            Assertions.assertEquals(consentToInsert, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    public static ConsentEntity newConsent(boolean accepted) {
        ConsentEntity c = new ConsentEntity();
        c.setAccepted(true);
        c.setCreated(Instant.now());
        c.setLastModified(Instant.now());
        return c;
    }

    @Test
    void getConsentByType() {

        //Given
        ConsentEntity consentToInsert = newConsent(false);
        ConsentEntity result = newConsent(false);
        //da usare ConsentTypeDto?
        consentToInsert.setSk("DATAPRIVACY");


        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));
        consentDao.getConsentByType(consentToInsert.getRecipientId(), consentToInsert.getSk());

        //Then

        try {

            Assertions.assertNotNull(result);
            Assertions.assertEquals(ConsentTypeDto.DATAPRIVACY, result.getSk());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());

            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void getConsents() {
        //devo verificare che per ogni recipientId ci siano due consensi (privacy e tos)
        //Given
        ConsentEntity consentToInsert = newConsent(false);
        consentToInsert.setSk("DATAPRIVACY");

        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));
        consentToInsert.setSk("TOS");

        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));

        List<ConsentEntity> results = (List<ConsentEntity>) consentDao.getConsents(consentToInsert.getRecipientId());

        //then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(2, results.size());
            Assertions.assertEquals("DATAPRIVACY", results.get(0).getSk());
            Assertions.assertEquals("TOS", results.get(1).getSk());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void updateConsent() {

        ConsentEntity consentToInsert = newConsent(false);

        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));

        //Then
        try {
            ConsentEntity elementFromDb1 = testDao.get(consentToInsert.getPk(), consentToInsert.getSk());


            consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));

            ConsentEntity elementFromDb2 = testDao.get(consentToInsert.getPk(), consentToInsert.getSk());

            Assertions.assertNotNull(elementFromDb1);
            Assertions.assertNotNull(elementFromDb2);
            Assertions.assertEquals(elementFromDb1.getCreated(), elementFromDb2.getCreated());
            Assertions.assertNotEquals(elementFromDb1.getLastModified(), elementFromDb2.getLastModified());

        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

}