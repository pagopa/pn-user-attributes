package it.pagopa.pn.user.attributes.middleware.db;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.user.attributes.LocalStackTestConfig;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@Import(LocalStackTestConfig.class)
@SpringBootTest
public class ConsentDaoTestIT {

    private final Duration d = Duration.ofMillis(3000);

    @Autowired
    private ConsentDao consentDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnUserattributesConfig pnUserattributesConfig;

    @MockBean
    PnAuditLogBuilder pnAuditLogBuilder;

    TestDao<ConsentEntity> testDao;

    @BeforeEach
    void setup() {
        testDao = new TestDao<>(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), ConsentEntity.class);
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
        consentDao.consentAction(consentToInsert).block(d);

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
        ConsentEntity c = new ConsentEntity("PF-123e4567-e89b-12d3-a456-426614174000", "TOS", null);
        c.setAccepted(accepted);
        c.setCreated(Instant.now());
        c.setLastModified(Instant.now());
        return c;
    }

    @Test
    void getConsentByType() {

        //Given
        ConsentEntity consentToInsert = newConsent(false);


        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
            consentDao.consentAction(consentToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        ConsentEntity result = consentDao.getConsentByType(consentToInsert.getRecipientId(), consentToInsert.getConsentType(), consentToInsert.getConsentVersion()).block(d);

        //Then

        try {
            Assertions.assertNotNull(result);
            Assertions.assertEquals(consentToInsert, result);
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
        List<ConsentEntity> toInsert = new ArrayList<>();
        ConsentEntity consentToInsert = newConsent(false);
        consentToInsert.setSk("DATAPRIVACY");
        toInsert.add(consentToInsert);
        consentToInsert = newConsent(true);
        consentToInsert.setSk("TOS");
        toInsert.add(consentToInsert);

        try {
            toInsert.forEach(x -> {
                try {
                    testDao.delete(x.getPk(), x.getSk());
                    consentDao.consentAction(x).block(d);
                } catch (Exception e) {
                    System.out.println("error removing");
                }
            });
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }


        //WHEN
        List<ConsentEntity> results = consentDao.getConsents(consentToInsert.getRecipientId()).collectList().block(d);

        //THEN
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(2, results.size());
            Assertions.assertTrue(toInsert.contains(results.get(0)));
            Assertions.assertTrue(toInsert.contains(results.get(1)));
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
        //GIVEN
        ConsentEntity consentToInsert = newConsent(false);
        ConsentEntity consentToUpdate = newConsent(true);
        consentToUpdate.setCreated(consentToUpdate.getCreated().plusMillis(100));
        consentToUpdate.setLastModified(consentToUpdate.getLastModified().plusMillis(100));

        try {
            testDao.delete(consentToInsert.getPk(), consentToInsert.getSk());
            consentDao.consentAction(consentToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //WHEN
        consentDao.consentAction(consentToUpdate).block(d);


        //Then
        try {
            ConsentEntity elementFromDb1 = testDao.get(consentToUpdate.getPk(), consentToUpdate.getSk());

            Assertions.assertNotNull(elementFromDb1);
            Assertions.assertEquals(consentToUpdate.getPk(), elementFromDb1.getPk());
            Assertions.assertEquals(consentToUpdate.getSk(), elementFromDb1.getSk());
            Assertions.assertEquals(consentToUpdate.isAccepted(), elementFromDb1.isAccepted());
            Assertions.assertEquals(consentToUpdate.getLastModified(), elementFromDb1.getLastModified());
            Assertions.assertEquals(consentToInsert.getCreated(), elementFromDb1.getCreated());


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