package utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public abstract class DateFormatUtils {
    public static final String yyyyMMddHHmmssSSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static String formatInstantToIso8601String(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.from(ZoneOffset.UTC));

        return formatter.format(instant);
    }
}