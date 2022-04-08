package it.pagopa.pn.user.attributes.utils;

import java.util.Base64;

public class EncodingUtils {
    private EncodingUtils() {}

    public static String base64Encoding(String elem){
        return Base64.getEncoder().encodeToString(elem.getBytes());
    }
}