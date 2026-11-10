package io.github.rogeriocisi.airlab.support;

import java.security.SecureRandom;

/**
 * Draws the seeds every commitment scheme of the platform starts from (Constitution P5).
 *
 * <p>One place, so that no protocol ever draws a seed from anything weaker than
 * {@link SecureRandom}. Expanding a seed into decisions is a different job, and a
 * deliberately reproducible one: {@link SeedStream}.
 */
public final class Seeds {

    private static final SecureRandom SOURCE = new SecureRandom();

    private Seeds() {
    }

    /** A fresh 32 byte seed in lower case hex. Kept secret until the protocol reveals it. */
    public static String newSeedHex() {
        byte[] seed = new byte[32];
        SOURCE.nextBytes(seed);
        return Hashes.toHex(seed);
    }
}
