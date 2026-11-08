package io.github.rogeriocisi.airlab.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 helpers used by the audit chain (P4), the preregistration commitment (P2)
 * and the allocation seed commitment (P5).
 *
 * <p>Everything here is plain JCA on purpose: no exotic crypto dependency (Constitution,
 * stack table). The hex encoding is lower case and stable across platforms, because an
 * external auditor must be able to recompute every value by hand.
 */
public final class Hashes {

    /** Chain anchor for the very first audit event. */
    public static final String ZERO = "0".repeat(64);

    private Hashes() {
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] sha256(byte[] value) {
        return digest().digest(value);
    }

    public static String toHex(byte[] value) {
        return HexFormat.of().formatHex(value);
    }

    public static byte[] fromHex(String value) {
        return HexFormat.of().parseHex(value);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by this platform", e);
        }
    }
}
