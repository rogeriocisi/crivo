package io.github.rogeriocisi.airlab.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic fingerprint of an identifiable value, so the system can ask "do we already know
 * this person?" without storing the value in the clear and without making it reversible.
 *
 * <p>Needed by the self-service study (Epic E10): the same person must not hold two roles in one
 * study, and with open enrolment collusion is the fraud that destroys a result without leaving
 * an obvious trace. Comparing encrypted contacts is impossible — {@link FieldCipher} uses a
 * random IV, so the same e-mail encrypts differently every time, which is exactly what you want
 * for confidentiality and exactly what defeats a lookup.
 *
 * <p>HMAC-SHA256 under a secret key, never a bare hash: a bare SHA-256 of an e-mail address is
 * trivially reversed with a wordlist. Without the key the fingerprint says nothing, and it is
 * never exported in the verification package (P6, P7).
 */
public final class BlindIndex {

    private final byte[] key;

    public BlindIndex(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("the blind index key must be at least 32 bytes");
        }
        this.key = key.clone();
    }

    /**
     * Fingerprint of a contact. Case, surrounding space and accents are normalised first, so
     * that the same address typed differently still matches.
     *
     * @param domain separates fingerprints by purpose, so a contact fingerprint cannot be
     *               compared against a fingerprint computed for anything else
     */
    public String of(String domain, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cannot fingerprint an empty value");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(domain.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            return HexFormat.of().formatHex(mac.doFinal(normalise(value).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            // No value in the message (P6).
            throw new IllegalStateException("blind index computation failed", e);
        }
    }

    private static String normalise(String value) {
        String folded = Normalizer.normalize(value.strip(), Normalizer.Form.NFKC);
        return folded.toLowerCase(Locale.ROOT);
    }
}
