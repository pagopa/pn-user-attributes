package it.pagopa.pn.user.attributes.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecipientIdUtilsTest {

    @Test
    void removeRecipientIdPrefixPf() {
        Assertions.assertEquals("abcd", RecipientIdUtils.removeRecipientIdPrefix("PF-abcd"));
    }

    @Test
    void removeRecipientIdPrefixPg() {
        Assertions.assertEquals("abcd", RecipientIdUtils.removeRecipientIdPrefix("PG-abcd"));
    }

    @Test
    void removeRecipientIdPrefixNotPrefixed() {
        // un codice fiscale non è prefissato, deve restare invariato
        Assertions.assertEquals("EEEEEE00E00E000A", RecipientIdUtils.removeRecipientIdPrefix("EEEEEE00E00E000A"));
    }

    @Test
    void removeRecipientIdPrefixNull() {
        Assertions.assertNull(RecipientIdUtils.removeRecipientIdPrefix(null));
    }

    @Test
    void removeRecipientIdPrefixTooShort() {
        // stringhe più corte del prefisso non devono generare eccezioni
        Assertions.assertEquals("PF-", RecipientIdUtils.removeRecipientIdPrefix("PF-"));
        Assertions.assertEquals("ab", RecipientIdUtils.removeRecipientIdPrefix("ab"));
        Assertions.assertEquals("", RecipientIdUtils.removeRecipientIdPrefix(""));
    }
}
