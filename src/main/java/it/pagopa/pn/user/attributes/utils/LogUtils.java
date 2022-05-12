package it.pagopa.pn.user.attributes.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtils {

    private LogUtils(){}

    public static String maskEmailAddress(String strEmail) {

        String[] parts = strEmail.split("@");

        //mask first part
        String strId = "";
        if(parts[0].length() < 4)
            strId = maskString(parts[0], 0, parts[0].length(), '*');
        else
            strId = maskString(parts[0], 1, parts[0].length()-1, '*');

        //now append the domain part to the masked id part
        return strId + "@" + parts[1];
    }

    public static String maskNumber(String number) {
        try {
            return maskString(number, 1, number.length() - 3, '*');
        } catch (Exception e) {
            log.error("cannot mask number", e);
            return "***";
        }
    }

    public static String maskString(String strText, int start, int end, char maskChar) {

        if(strText == null || strText.equals(""))
            return "";

        if(start < 0)
            start = 0;

        if( end > strText.length() )
            end = strText.length();


        int maskLength = end - start;

        if(maskLength == 0)
            return strText;

        StringBuilder sbMaskString = new StringBuilder(maskLength);

        for(int i = 0; i < maskLength; i++){
            sbMaskString.append(maskChar);
        }

        return strText.substring(0, start)
                + sbMaskString.toString()
                + strText.substring(start + maskLength);
    }
}
