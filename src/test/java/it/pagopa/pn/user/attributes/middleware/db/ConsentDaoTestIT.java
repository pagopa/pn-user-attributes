package it.pagopa.pn.user.attributes.middleware.db;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;

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
        testDao = new TestDao<ConsentEntity>( dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), ConsentEntity.class);
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

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertEquals( consentToInsert, elementFromDb);
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
        consentToInsert.setSk("Privacy");


        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        consentDao.consentAction(consentToInsert).block(Duration.ofMillis(3000));

        //Then
        try {
            ConsentEntity result = testDao.get(consentToInsert.getRecipientId(), consentToInsert.getSk());

            Assertions.assertNotNull(result);
            Assertions.assertEquals("Privacy", result.getSk());
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
    }
}