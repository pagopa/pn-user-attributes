package it.pagopa.pn.user.attributes.utils;

import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.server.v1.dto.CxLanguageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageUtilsTest {

    @Test
    void resolveLanguage_null_returnsIT() {
        assertEquals(LanguageEnum.IT, LanguageUtils.resolveLanguage(null));
    }

    @Test
    void resolveLanguage_IT_returnsIT() {
        assertEquals(LanguageEnum.IT, LanguageUtils.resolveLanguage(CxLanguageDto.IT));
    }

    @Test
    void resolveLanguage_DE_returnsDE() {
        assertEquals(LanguageEnum.DE, LanguageUtils.resolveLanguage(CxLanguageDto.DE));
    }

    @Test
    void resolveLanguage_SL_returnsSL() {
        assertEquals(LanguageEnum.SL, LanguageUtils.resolveLanguage(CxLanguageDto.SL));
    }

    @Test
    void resolveLanguage_FR_returnsFR() {
        assertEquals(LanguageEnum.FR, LanguageUtils.resolveLanguage(CxLanguageDto.FR));
    }

    @Test
    void resolveLanguage_EN_fallbackToIT() {
        assertEquals(LanguageEnum.IT, LanguageUtils.resolveLanguage(CxLanguageDto.EN));
    }
}
