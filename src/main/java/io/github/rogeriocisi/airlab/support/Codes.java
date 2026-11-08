package io.github.rogeriocisi.airlab.support;

import java.security.SecureRandom;

/**
 * Pseudonymous codes and opaque references (P1, P6).
 *
 * <p>Codes carry no meaning and no order: they must not let anyone infer who a participant
 * is, when a row was created, or which reading belongs to whom. That rules out sequential
 * ids and timestamps in any identifier that leaves the application.
 */
public final class Codes {

    /** Crockford base32 without I, L, O, U: no digit/letter confusion when read aloud. */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private Codes() {
    }

    /** A code such as {@code SIT-7Q4K2M9B}. The prefix names the kind, never the person. */
    public static String of(String prefix) {
        return prefix + "-" + random(8);
    }

    /** Random storage reference for uploaded media: no original file name, ever (P1). */
    public static String mediaRef() {
        return random(24);
    }

    /** Single use access token for sitter and rater links. Only its hash is persisted. */
    public static String accessToken() {
        return random(32);
    }

    private static String random(int length) {
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            out.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return out.toString();
    }
}
