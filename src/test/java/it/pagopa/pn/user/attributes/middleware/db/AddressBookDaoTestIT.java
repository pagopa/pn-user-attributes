package it.pagopa.pn.user.attributes.middleware.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566"
})
@SpringBootTest
class AddressBookDaoTestIT {

    @Test
    void deleteAddressBook() {

    }

    @Test
    void getAddresses() {
    }

    @Test
    void getAllAddressesByRecipient() {
    }

    @Test
    void saveVerificationCode() {
    }

    @Test
    void getVerificationCode() {
    }

    @Test
    void validateHashedAddress() {
    }

    @Test
    void saveAddressBookAndVerifiedAddress() {
    }
}