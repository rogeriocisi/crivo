package io.github.rogeriocisi.airlab.trials;

import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.SeedStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Derives the target of each trial from the session seed (Epics E11 and E12).
 *
 * <p>This is the whole trick of the self-service tests, and it is deliberately four lines of
 * arithmetic that anyone can reimplement:
 *
 * <pre>
 * alvo(n) = SeedStream(semente, "trial:" + n).nextInt(alternativas)
 * </pre>
 *
 * <p>The seed is committed by hash before the first trial and revealed when the session ends.
 * So a person who finishes a session can recompute every target from the published seed and
 * check that none of them was picked after their answer. The platform does not ask to be
 * trusted on that point; it hands over the means to check.
 *
 * <p>Pure function: no clock, no database, no randomness beyond the seed. The same seed gives
 * the same targets on any machine, which is what makes the session auditable at all.
 */
public final class TargetDerivation {

    private TargetDerivation() {
    }

    /**
     * Combines the seed the server committed to with the nonce the participant's browser drew,
     * into the seed every target of the session comes from:
     *
     * <pre>
     * semente = SHA-256( bytes(sementeDoServidor) || bytes(nonce) )
     * </pre>
     *
     * <p>This is what removes the need to trust us. The server publishes a commitment to its
     * half before it has seen the nonce; the participant draws the nonce after seeing that
     * commitment. Neither side can steer the result: the server would have to find a second
     * half that hashes with this nonce to a seed it likes, which is a preimage attack on
     * SHA-256, and the participant never learns the server's half until the session closes.
     *
     * <p>Test vector, pinned in Java and repeated in the browser verifier:
     * server seed of 32 bytes {@code 0x11}, nonce of 16 bytes {@code 0x22}, effective seed
     * {@code 067163355c1ef281e5b95dd6000e3945f41154808a1f14698b9a2daa3d89079f}.
     */
    public static String effectiveSeedHex(String serverSeedHex, String participantNonceHex) {
        byte[] serverSeed = Hashes.fromHex(serverSeedHex);
        byte[] nonce = Hashes.fromHex(participantNonceHex);
        byte[] joined = new byte[serverSeed.length + nonce.length];
        System.arraycopy(serverSeed, 0, joined, 0, serverSeed.length);
        System.arraycopy(nonce, 0, joined, serverSeed.length, nonce.length);
        return Hashes.toHex(Hashes.sha256(joined));
    }

    /** Index of the target alternative of a trial, in {@code [0, alternatives)}. */
    public static int targetIndex(String seedHex, int trialIndex, int alternatives) {
        if (trialIndex < 1) {
            throw new IllegalArgumentException("trial index starts at 1");
        }
        if (alternatives < 2) {
            throw new IllegalArgumentException("a forced choice needs at least two alternatives");
        }
        return new SeedStream(Hashes.fromHex(seedHex), "trial:" + trialIndex).nextInt(alternatives);
    }

    /**
     * The candidate figures of a remote viewing trial: the target plus decoys, drawn from a
     * versioned pool, in the order they are shown.
     *
     * <p>Drawn from the same seed and in a separate domain, so that adding this protocol did
     * not shift the targets any earlier protocol derives from its own seed.
     *
     * @param poolSize   how many figures the versioned pool has
     * @param candidates how many figures are shown, target included
     * @return pool indices in presentation order; the target is at
     *         {@link #targetIndex(String, int, int)} of this list
     */
    public static List<Integer> candidatePoolIndices(String seedHex, int trialIndex,
                                                     int poolSize, int candidates) {
        if (candidates < 2) {
            throw new IllegalArgumentException("a ranking needs at least two candidates");
        }
        if (poolSize < candidates) {
            throw new IllegalArgumentException(
                    "the target pool has " + poolSize + " figures, fewer than the "
                            + candidates + " a trial shows");
        }
        List<Integer> pool = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(i);
        }
        // Shuffling the whole pool and taking the head keeps the draw uniform over subsets and
        // independent of the order the pool happened to be loaded in (P5).
        new SeedStream(Hashes.fromHex(seedHex), "pool:" + trialIndex).shuffle(pool);
        return List.copyOf(pool.subList(0, candidates));
    }
}
