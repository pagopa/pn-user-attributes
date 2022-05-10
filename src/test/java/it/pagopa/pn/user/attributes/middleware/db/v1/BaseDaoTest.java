package it.pagopa.pn.user.attributes.middleware.db.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BaseDaoTest {
    private static final String PK = "testPK";
    private static final String SK = "testSK";

    private BaseDao dao;

    @BeforeEach
    void setUp() {
        dao = new BaseDao();
    }

    @Test
    void getKeyBuild1() {

        Key k = dao.getKeyBuild(PK);
        AttributeValue a = k.partitionKeyValue();
        String pk = a.s();

        assertEquals(PK, pk);
    }

    @Test
    void GetKeyBuild2() {
        Key k = dao.getKeyBuild(PK, SK);

        AttributeValue ap = k.partitionKeyValue();
        String pk = ap.s();
        assertEquals(PK, pk);

        Optional<AttributeValue> as = k.sortKeyValue();
        String sk = as.get().s();

        assertEquals(SK, sk);
    }

}