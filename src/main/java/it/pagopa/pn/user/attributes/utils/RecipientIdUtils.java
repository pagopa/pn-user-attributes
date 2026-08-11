package it.pagopa.pn.user.attributes.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipientIdUtils {

    private static final int PREFIX_SEPARATOR_INDEX = 2;
    private static final int PREFIX_LENGTH = 3;

    /**
     * Rimuove il prefisso di tipologia dal recipientId, ottenendo lo uid di auth-fleet.
     * <p>
     * Per le persone fisiche auth-fleet valorizza {@code x-pagopa-pn-cx-id} con
     * {@code "PF-" + uid}, dove {@code uid} è l'identificativo privo di prefisso: la
     * conversione inversa serve quindi ogni volta che a partire dal recipientId si deve
     * ricavare lo uid, ad esempio per la lettura dei consensi o per valorizzare l'header
     * {@code x-pagopa-pn-uid} verso gli altri microservizi.
     *
     * @param recipientId recipientId del destinatario (es. {@code PF-<uuid>})
     * @return lo uid privo di prefisso, oppure il valore ricevuto se non è prefissato
     */
    public static String removeRecipientIdPrefix(String recipientId) {
        if (recipientId != null
                && recipientId.length() > PREFIX_LENGTH
                && recipientId.charAt(PREFIX_SEPARATOR_INDEX) == '-') {
            return recipientId.substring(PREFIX_LENGTH);
        }
        return recipientId;
    }
}