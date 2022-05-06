package it.pagopa.pn.user.attributes.middleware.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
/*
 * I metodi di test (@Test) verificano ciascuno un solo metodo di ConsentDao
 */
class ConsentDaoTestIT {
    /*private static final String RECIPIENTID = "123e4567-e89b-12d3-a456-426614174007";
    private static final String RECIPIENTID_NOT_FOUND = "not-existent-recid";

    private static final String CONSENTTYPE = "TOS";

    private static final boolean ISACCEPTED = true;


    private IConsentDao consentDao;

    private DynamoDbAsyncTable<ConsentEntity> userAttributesTable;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @BeforeEach
    void setUp(@Value(ConsentDao.DYNAMODB_TABLE_NAME) String table) {
        consentDao = new ConsentDao(dynamoDbEnhancedAsyncClient, table);
        userAttributesTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(ConsentEntity.class));

        // cancello il record in database (se esistente)
        try {
            deleteItem(RECIPIENTID, CONSENTTYPE);
        } catch (Exception e) {
            fail(e);
        }

    }

    @AfterEach
    void deleteRow() {
        // cancello il record in database (se esistente)
        try {
            deleteItem(RECIPIENTID, CONSENTTYPE);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    // Creazione di un consenso
    void consentAction() {
        // GIVEN
        ConsentEntity consentEntity = getNewConsentEntity(Instant.now(), Instant.now(), ISACCEPTED);
        ConsentTypeDto ctDto = ConsentTypeDto.TOS;

        // WHEN
        // scrivo il record in database
        try {
            consentDao.consentAction(consentEntity).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            fail(e);
        }

        // THEN
        // leggo il consenso associato a recipientId e ConsentTypeDto
        try {
            ConsentEntity ret = getConsentByType(RECIPIENTID, ctDto.getValue());
            assertNotNull(ret);
            assertNotNull(ret.getCreated());
            assertNull(ret.getLastModified());
            assertEquals(consentEntity.getConsentType(),ret.getConsentType());
            assertEquals(consentEntity.getRecipientId(),ret.getRecipientId());
            assertEquals(consentEntity.isAccepted(),ret.isAccepted());
        } catch (Exception e) {
            fail(e);
        }

    }


    @Test
        // Modifica di un consenso
    void consentAction_Update() {
        // GIVEN
        ConsentEntity consentEntity = getNewConsentEntity(Instant.now(), null, ISACCEPTED);
        ConsentTypeDto ctDto = ConsentTypeDto.TOS;

        // scrivo il record in database
        try {
            consentAction(consentEntity);
        } catch (Exception e) {
            fail(e);
        }

        // WHEN
        // scrivo il record in database
        try {
            consentEntity.setAccepted(!consentEntity.isAccepted());
            consentEntity.setLastModified(Instant.now());
            consentDao.consentAction(consentEntity).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            fail(e);
        }


        // THEN
        // leggo il consenso associato a recipientId e ConsentTypeDto
        // diversamente da prima getLastModified() != null e ISACCEPTED = false
        try {
            ConsentEntity ret = getConsentByType(RECIPIENTID, ctDto.getValue());
            assertNotNull(ret);
            assertNotNull(ret.getCreated());
            assertNotNull(ret.getLastModified());
            assertEquals(consentEntity.getConsentType(),ret.getConsentType());
            assertEquals(consentEntity.getRecipientId(),ret.getRecipientId());
            assertEquals(consentEntity.isAccepted(),ret.isAccepted());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getConsentByType() {
        // GIVEN
        ConsentEntity consentEntity = getNewConsentEntity(Instant.now(), Instant.now(), ISACCEPTED);
        ConsentTypeDto ctDto = ConsentTypeDto.TOS;

        // WHEN
        // scrivo il record in database
        try {
            consentAction(consentEntity);
        } catch (Exception e) {
            fail(e);
        }

        // THEN
        // leggo il consenso associato a recipientId e ConsentTypeDto
        try {
            ConsentEntity ret = consentDao.getConsentByType(RECIPIENTID, ctDto).block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertNotNull(ret.getCreated());
            assertNotNull(ret.getLastModified());
            assertEquals(consentEntity.getConsentType(),ret.getConsentType());
            assertEquals(consentEntity.getRecipientId(),ret.getRecipientId());
            assertEquals(consentEntity.isAccepted(),ret.isAccepted());
        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void getConsentByType_Not_Found() {
        // GIVEN
        ConsentTypeDto ctDto = ConsentTypeDto.TOS;

        // WHEN
        // il record non esiste perchè cancellato in setUp() -> non faccio niente

        // THEN
        // leggo il consenso associato a recipientId e ConsentTypeDto
        try {
            ConsentEntity ret = consentDao.getConsentByType(RECIPIENTID, ctDto).block(Duration.ofMillis(3000));
            assertNull(ret);
        } catch (Exception e) {
            fail(e);
        }

    }
    @Test
    void getConsents() {
        // GIVEN
        ConsentEntity consentEntity = getNewConsentEntity(Instant.now(), Instant.now(), ISACCEPTED);

        // WHEN
        // scrivo il record in database
        try {
            consentAction(consentEntity);
        } catch (Exception e) {
            fail(e);
        }

        // THEN
        // leggo la lista dei consensi (size = 1)
        try {
            List<ConsentEntity> ret = consentDao.getConsents(RECIPIENTID).collectList().block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertTrue(ret.size() > 0);
        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void getConsent_Not_Found() {
        // GIVEN
        ConsentEntity consentEntity = getNewConsentEntity(Instant.now(), Instant.now(), ISACCEPTED);
        consentEntity.setRecipientId(ConsentEntity.getPk(RECIPIENTID_NOT_FOUND));

       // THEN

        //WHEN
        // leggo la lista dei consensi (size = 0)
        try {
            List<ConsentEntity> ret = consentDao.getConsents(RECIPIENTID).collectList().block(Duration.ofMillis(3000));
            assertNotNull(ret);
            assertEquals(0, ret.size());
        } catch (Exception e) {
            fail(e);
        }

    }

    private ConsentEntity deleteItem(String recipipientId, String consentType) throws ExecutionException, InterruptedException {
            String pk = ConsentEntity.getPk(recipipientId);
            DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                    .key(Key.builder().partitionValue(pk).sortValue(consentType).build())
                    .build();

            return userAttributesTable.deleteItem(req).get();
    }

    private ConsentEntity getConsentByType(String recipipientId, String consentType) throws ExecutionException, InterruptedException {
            String pk = ConsentEntity.getPk(recipipientId);
            GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                    .key(Key.builder().partitionValue(pk).sortValue(consentType).build())
                    .build();

            return userAttributesTable.getItem(req).get();
    }

    private ConsentEntity consentAction(ConsentEntity userAttributes) throws ExecutionException, InterruptedException {
        UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                .item(userAttributes)
                .ignoreNulls(true)
                .build();
        return userAttributesTable.updateItem(updRequest).get();
    }

    private static ConsentEntity getNewConsentEntity (Instant created, Instant lastModified, boolean accepted) {
        String pk = ConsentEntity.getPk(RECIPIENTID);
        ConsentEntity consentEntity = ConsentEntity
                .builder()
                .recipientId(pk)
                .consentType(CONSENTTYPE)
                .created(created)
                .lastModified(lastModified)
                .accepted(accepted)
                .build();
        return consentEntity;
    }
    */
}
