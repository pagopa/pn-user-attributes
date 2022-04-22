package it.pagopa.pn.user.attributes.middleware.db.v1;

import software.amazon.awssdk.enhanced.dynamodb.Key;

public class BaseDao {

    protected Key getKeyBuild(String pk) {
        return getKeyBuild(pk, null);
    }

    protected Key getKeyBuild(String pk, String sk) {
        if (sk == null)
                return Key.builder().partitionValue(pk).build();
        else
                return Key.builder().partitionValue(pk).sortValue(sk).build();
    }

}
