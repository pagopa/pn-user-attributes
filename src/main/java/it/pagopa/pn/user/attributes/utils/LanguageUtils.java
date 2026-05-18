package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxLanguageDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LanguageUtils {

    private LanguageUtils() {}

    public static LanguageEnum resolveLanguage(CxLanguageDto headerValue) {
        if (headerValue == null) {
            log.warn("language header fallback to IT");
            return LanguageEnum.IT;
        }
        try {
            return LanguageEnum.fromValue(headerValue.getValue());
        } catch (IllegalArgumentException e) {
            log.warn("language header fallback to IT");
            return LanguageEnum.IT;
        }
    }
}
