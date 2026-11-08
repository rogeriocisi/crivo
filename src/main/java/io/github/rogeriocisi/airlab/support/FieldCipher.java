package io.github.rogeriocisi.airlab.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM for data at rest: identifiable fields of the {@code identity} schema (P6) and
 * the escrowed draw seed (P5).
 *
 * <p>Plain JCA, no exotic dependency. The stored form is {@code v1:base64(iv || ciphertext)},
 * version prefixed so a future key rotation can be told apart from existing rows.
 */
public final class FieldCipher {

    private static final String STORED_PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    /** @param key256 exactly 32 bytes */
    public FieldCipher(byte[] key256) {
        if (key256 == null || key256.length != 32) {
            throw new IllegalArgumentException("the encryption key must be 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(key256, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] stored = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, stored, 0, iv.length);
            System.arraycopy(ciphertext, 0, stored, iv.length, ciphertext.length);
            return STORED_PREFIX + Base64.getEncoder().encodeToString(stored);
        } catch (GeneralSecurityException e) {
            // No value and no field name in the message (P6).
            throw new IllegalStateException("field encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(STORED_PREFIX)) {
            throw new IllegalStateException("stored value is not in the expected envelope format");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(stored.substring(STORED_PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(raw, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(raw, IV_BYTES, raw.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("field decryption failed", e);
        }
    }
}
