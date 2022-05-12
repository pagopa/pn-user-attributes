package it.pagopa.pn.user.attributes.middleware.db;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerifiedAddressEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566"
})
@SpringBootTest
class AddressBookDaoTestIT {
    private final Duration d = Duration.ofMillis(3000);

    @Autowired
    private AddressBookDao addressBookDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnUserattributesConfig pnUserattributesConfig;

    TestDao<AddressBookEntity> testDao;

    @BeforeEach
    void setup() {
        testDao = new TestDao(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), AddressBookEntity.class);
    }

    @Test
    void deleteAddressBook() {

        //Given
        AddressBookEntity addressBookToDelete = newAddress();
        VerifiedAddressEntity verifiedAddress = new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000", "Legal", "SMS");
        try {
            testDao.delete(addressBookToDelete.getPk(), addressBookToDelete.getSk());
            addressBookDao.saveAddressBookAndVerifiedAddress(addressBookToDelete,verifiedAddress).block(d);
        } catch (Exception e) {
            System.out.println("error removing");
        }

        //When
        addressBookDao.deleteAddressBook(addressBookToDelete.getRecipientId(), addressBookToDelete.getSenderId(), addressBookToDelete.getAddressType(), addressBookToDelete.getChannelType());

        //Then
        try {
            AddressBookEntity elementFromDb = testDao.get(addressBookToDelete.getPk(), addressBookToDelete.getSk());
            Assertions.assertNull(elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(addressBookToDelete.getPk(), addressBookToDelete.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }

        }
    }
        @Test
        void getAddresses () {

                //Given
                VerifiedAddressEntity verifiedAddress =new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000","Legal","SMS");
                List<AddressBookEntity> toInsert = new ArrayList<>();
                AddressBookEntity addressToInsert = newAddress();
                addressToInsert.setSk("Legal");
                toInsert.add(addressToInsert);
                addressToInsert = newAddress();
                addressToInsert.setSk("courtesy");
                toInsert.add(addressToInsert);

                try {
                    toInsert.forEach(x -> {
                        try {
                            testDao.delete(x.getPk(), x.getSk());
                            addressBookDao.saveAddressBookAndVerifiedAddress(x,verifiedAddress ).block(d);
                        } catch (Exception e) {
                            System.out.println("error removing");
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }

                //WHEN
            List<AddressBookEntity> results = addressBookDao.getAddresses(addressToInsert.getRecipientId(),addressToInsert.getSenderId(), addressToInsert.getAddressType()).collectList().block(d);

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
                        testDao.delete(addressToInsert.getPk(), addressToInsert.getSk());
                    } catch (Exception e) {
                        System.out.println("Nothing to remove");
                    }
                }
                }


        @Test
        void getAllAddressesByRecipient () {
        }

        @Test
        void saveVerificationCode () {
        }

        @Test
        void getVerificationCode () {
        }

        @Test
        void validateHashedAddress() {

            //Given
            AddressBookEntity addressBook = newAddress();
            VerifiedAddressEntity verifiedAddress = new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000", "hashAddressgahs67323525", "SMS");
            try {
                testDao.delete(addressBook.getPk(), addressBook.getSk());
                addressBookDao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddress).block(d);
            } catch (Exception e) {
                System.out.println("error removing");
            }

            //When
            AddressBookDao.CHECK_RESULT result = addressBookDao.validateHashedAddress(addressBook.getRecipientId(), "hashAddressgahs67323525WRONG").block(d);

            //Then
            try {
                Assertions.assertEquals(AddressBookDao.CHECK_RESULT.NOT_EXISTS, result);
            } catch (Exception e) {
                fail(e);
            } finally {
                try {
                    testDao.delete(addressBook.getPk(), addressBook.getSk());
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }
            }
        }

        @Test
        void saveAddressBookAndVerifiedAddress () {
        }

        public static AddressBookEntity newAddress() {
            AddressBookEntity ab = new AddressBookEntity("AB-123e4567-e89b-12d3-a456-426614174000", "LEGAL", "default", "SMS");
            ab.setCreated(Instant.now());
            ab.setLastModified(Instant.now());
            return ab;
        }
    }
