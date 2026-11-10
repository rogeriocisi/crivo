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
