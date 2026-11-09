package io.github.rogeriocisi.airlab.scoring;

import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Instant;
import java.util.Optional;

/**
 * The single-use, expiring link by which a consulente reaches their scoring packet
 * (Epic E6, Epic E10).
 *
 * <p>No account, no password: asking a bereaved person to create an account to rate two
 * readings would cost participation and gain nothing. The link is the credential, so it is
 * treated like one — generated from {@link java.security.SecureRandom}, stored only as a
 * SHA-256 hash, and refused once the packet is locked or the deadline has passed.
 *
 * <p>Storing only the hash matters more here than for a password: the token travels by e-mail,
 * and whoever holds a database dump must not be able to open someone's packet with it.
 */
public final class AccessLink {

    private AccessLink() {
    }

    /**
     * A freshly minted token and the hash to persist.
     *
     * @param token the value that goes into the e-mail. Never logged, never stored, never
     *              returned by any query afterwards
     */
    public record Issued(String token, String tokenHash, Instant expiresAt) {

        @Override
        public String toString() {
            // Overridden so an accidental log statement cannot leak the credential.
            return "Issued(token=redacted, expiresAt=" + expiresAt + ")";
        }
    }

    /** Why a link was refused, in terms the page can show without hinting at the content. */
    public enum Refusal {
        UNKNOWN_TOKEN,
        EXPIRED,
        ALREADY_SUBMITTED
    }

    public static Issued issue(Instant now, java.time.Duration validFor) {
        if (validFor.isNegative() || validFor.isZero()) {
            throw new IllegalArgumentException("a link needs a positive validity window");
        }
        String token = Codes.accessToken();
        return new Issued(token, hash(token), now.plus(validFor));
    }

    /** The value to compare against the stored column. */
    public static String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("an empty token is not a token");
        }
        return Hashes.sha256Hex(token);
    }

    /**
     * Decides whether a presented token may open the packet.
     *
     * @param storedHash  hash held for the packet, or null when no packet matches
     * @param lockedAt    when the packet was submitted, or null while it is open
     * @return empty when the link may be opened, otherwise the reason it was refused
     */
    public static Optional<Refusal> refuse(String presentedToken, String storedHash,
                                           Instant expiresAt, Instant lockedAt, Instant now) {
        if (storedHash == null || !constantTimeEquals(storedHash, hash(presentedToken))) {
            return Optional.of(Refusal.UNKNOWN_TOKEN);
        }
        if (lockedAt != null) {
            // Checked before expiry: "you already submitted" is the truer message, and a
            // submitted packet must never reopen, deadline or not.
            return Optional.of(Refusal.ALREADY_SUBMITTED);
        }
        if (!now.isBefore(expiresAt)) {
            return Optional.of(Refusal.EXPIRED);
        }
        return Optional.empty();
    }

    /** Comparison that does not return early, so timing does not reveal a partial match. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < a.length(); i++) {
            difference |= a.charAt(i) ^ b.charAt(i);
        }
        return difference == 0;
    }
}
