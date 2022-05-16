package it.pagopa.pn.user.attributes.middleware.db;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.entities.AddressBookEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.ConsentEntity;
import it.pagopa.pn.user.attributes.middleware.db.entities.VerificationCodeEntity;
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
public
class AddressBookDaoTestIT {
    private final Duration d = Duration.ofMillis(3000);

    @Autowired
    private AddressBookDao addressBookDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnUserattributesConfig pnUserattributesConfig;

    TestDao<AddressBookEntity> testDao;
    TestDao<VerificationCodeEntity> testCodeDao;


    @BeforeEach
    void setup() {
        testDao = new TestDao<>(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), AddressBookEntity.class);
        testCodeDao = new TestDao<>(dynamoDbEnhancedAsyncClient, pnUserattributesConfig.getDynamodbTableName(), VerificationCodeEntity.class);
    }

    @Test
    void deleteAddressBook() {

        //Given
        AddressBookEntity addressBookToDelete = newAddress(true);
        VerifiedAddressEntity verifiedAddress = new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000", "Legal", "SMS");
        try {
            testDao.delete(addressBookToDelete.getPk(), addressBookToDelete.getSk());
            addressBookDao.saveAddressBookAndVerifiedAddress(addressBookToDelete,verifiedAddress).block(d);
        } catch (Exception e) {
            System.out.println("error removing");
        }

        //When
        addressBookDao.deleteAddressBook(addressBookToDelete.getRecipientId(), addressBookToDelete.getSenderId(), addressBookToDelete.getAddressType(), addressBookToDelete.getChannelType()).block(d);

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
                toInsert.add(newAddress(true));
                toInsert.add(newAddress(false));



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
            List<AddressBookEntity> results = addressBookDao.getAddresses(toInsert.get(0).getRecipientId(),toInsert.get(0).getSenderId(), toInsert.get(0).getAddressType()).collectList().block(d);

                //THEN
                try {
                    Assertions.assertNotNull(results);
                    Assertions.assertEquals(1, results.size());
                    Assertions.assertTrue(toInsert.contains(results.get(0)));
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    try {
                        toInsert.forEach(x -> {
                            try {
                                testDao.delete(x.getPk(), x.getSk());
                            } catch (Exception e) {
                                System.out.println("error removing");
                            }
                        });
                    } catch (Exception e) {
                        System.out.println("Nothing to remove");
                    }
                }
                }


        @Test
        void getAllAddressesByRecipient () {
            //Given
            VerifiedAddressEntity verifiedAddress =new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000","Legal","SMS");
            List<AddressBookEntity> toInsert = new ArrayList<>();
            toInsert.add(newAddress(true));
            toInsert.add(newAddress(false));
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
            List<AddressBookEntity> results = addressBookDao.getAllAddressesByRecipient(toInsert.get(0).getRecipientId()).collectList().block(d);

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
                    toInsert.forEach(x -> {
                        try {
                            testDao.delete(x.getPk(), x.getSk());
                        } catch (Exception e) {
                            System.out.println("error removing");
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }
            }
        }


        @Test
        void saveVerificationCode () {
            //Given
            VerificationCodeEntity verificationCodeToInsert= new VerificationCodeEntity("VC-123e4567-e89b-12d3-a456-426614178000", "Address345678", "SMS");

            try {
                testDao.delete(verificationCodeToInsert.getPk(), verificationCodeToInsert.getSk());
            } catch (Exception e) {
                System.out.println("error removing");
            }
            //When
            addressBookDao.saveVerificationCode(verificationCodeToInsert).block(d);
            //Then
            try {
                VerificationCodeEntity verificationCodeFromDb = testCodeDao.get(verificationCodeToInsert.getPk(), verificationCodeToInsert.getSk());

                Assertions.assertNotNull( verificationCodeFromDb);
                Assertions.assertEquals( verificationCodeToInsert, verificationCodeFromDb);


            } catch (Exception e) {
                fail(e);
            } finally {
                try {
                    testDao.delete(verificationCodeToInsert.getPk(), verificationCodeToInsert.getSk());
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }
            }


        }

        @Test
        void getVerificationCode () {

            //Given
            VerificationCodeEntity verificationCodeToInsert= new VerificationCodeEntity("VC-123e4567-e89b-12d3-a456-426614178000", "address345678", "SMS");

            try {
                testDao.delete(verificationCodeToInsert.getPk(), verificationCodeToInsert.getSk());
                addressBookDao.saveVerificationCode(verificationCodeToInsert).block(d);
            } catch (Exception e) {
                System.out.println("error removing");
            }
            //When
            VerificationCodeEntity verificationCode = addressBookDao.getVerificationCode(verificationCodeToInsert).block(d);
            //Then
            try {
                Assertions.assertNotNull(verificationCode);
                Assertions.assertEquals( verificationCodeToInsert, verificationCode);
            } catch (Exception e) {
                fail(e);
            } finally {
                try {
                    testDao.delete(verificationCodeToInsert.getPk(), verificationCodeToInsert.getSk());
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }
            }

        }

        @Test
        void validateHashedAddress() {

            //Given
            AddressBookEntity addressBook = newAddress(true);
            VerifiedAddressEntity verifiedAddress = new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000", "hashAddressgahs67323525", "SMS");
            try {
                testDao.delete(addressBook.getPk(), addressBook.getSk());
                addressBookDao.saveAddressBookAndVerifiedAddress(addressBook, verifiedAddress).block(d);
            } catch (Exception e) {
                System.out.println("error removing");
            }

            //When
            AddressBookDao.CHECK_RESULT result1 = addressBookDao.validateHashedAddress(verifiedAddress.getRecipientId(), "hashAddressgahs67323525WRONG").block(d);
            AddressBookDao.CHECK_RESULT result2 = addressBookDao.validateHashedAddress(verifiedAddress.getRecipientId(), "hashAddressgahs67323525").block(d);
            //Then
            try {
                Assertions.assertEquals(AddressBookDao.CHECK_RESULT.NOT_EXISTS, result1);
                Assertions.assertEquals(AddressBookDao.CHECK_RESULT.ALREADY_VALIDATED,result2);

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
            //Given
            AddressBookEntity addressBookToInsert = newAddress(true);
            VerifiedAddressEntity verifiedAddressToInsert = new VerifiedAddressEntity("VA-123e4567-e89b-12d3-a456-426614174000", "address67323525", "SMS");
            try {
                testDao.delete(addressBookToInsert.getPk(), addressBookToInsert.getSk());
            } catch (Exception e) {
                System.out.println("error removing");
            }

            //When
            addressBookDao.saveAddressBookAndVerifiedAddress(addressBookToInsert, verifiedAddressToInsert).block(d);
            //Then
            try {
                AddressBookEntity addressBookFromDb = testDao.get(addressBookToInsert.getPk(),addressBookToInsert.getSk());

                Assertions.assertNotNull( addressBookFromDb);
                Assertions.assertEquals( addressBookToInsert, addressBookFromDb);

            } catch (Exception e) {
                fail(e);
            } finally {
                try {
                    testDao.delete(addressBookToInsert.getPk(), addressBookToInsert.getSk());
                } catch (Exception e) {
                    System.out.println("Nothing to remove");
                }
            }


        }

        public static AddressBookEntity newAddress(boolean isLegal) {
            if (isLegal)
                return new AddressBookEntity("123e4567-e89b-12d3-a456-426714174000", "LEGAL", "default", "PEC");
            else
                return new AddressBookEntity("123e4567-e89b-12d3-a456-426714174000", "COURTESY", "default", "EMAIL");
        }
    }
