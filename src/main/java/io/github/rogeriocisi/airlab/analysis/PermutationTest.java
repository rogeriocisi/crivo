package io.github.rogeriocisi.airlab.analysis;

import io.github.rogeriocisi.airlab.support.SeedStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Permutation test on a Pratt and Birge matrix: the analysis the field endorses most widely
 * for reading-accuracy data (Epic E7).
 *
 * <p>Each row is a sitter, each column a reading, and the cell is the score that sitter gave
 * that reading. The diagonal holds the scores sitters gave <em>their own</em> target reading.
 * The test asks how unusual the observed diagonal sum is among all the diagonal sums that
 * would arise if the readings were assigned to sitters differently.
 *
 * <p>Why it matters here: as O'Keeffe and Wiseman set out, this analysis does not assume that
 * the statements inside a reading are independent. The historical alternative, multiplying the
 * individual probability of each item, inflates accuracy badly, because the items of a reading
 * are correlated by construction ("he was male" and "he had a beard"). That multiplication is
 * a forbidden analysis in this project; see {@code docs/REFERENCES.md}.
 *
 * <p>Exact by full enumeration of the n! assignments up to n = 8 (40320 permutations). Above
 * that it samples permutations from a seed, so the number is reproducible by anyone holding
 * the revealed seed, exactly like the draw itself (P5, P7).
 */
public final class PermutationTest {

    private static final MathContext PRECISION = new MathContext(40, RoundingMode.HALF_EVEN);
    private static final int EXACT_LIMIT = 8;
    private static final int SAMPLED_PERMUTATIONS = 100_000;

    private PermutationTest() {
    }

    /**
     * @param scores square matrix, {@code scores[sitter][reading]}; the diagonal is each
     *               sitter's own target reading
     * @param seedHex seed used only when the matrix is too large to enumerate; may be null
     *                for small matrices
     */
    public static PermutationResult diagonalExceedsChance(double[][] scores, String seedHex) {
        int n = validate(scores);
        double observed = 0.0;
        for (int i = 0; i < n; i++) {
            observed += scores[i][i];
        }

        if (n <= EXACT_LIMIT) {
            long[] counters = new long[2];
            enumerate(scores, new int[n], new boolean[n], 0, observed, counters);
            BigDecimal p = BigDecimal.valueOf(counters[0])
                    .divide(BigDecimal.valueOf(counters[1]), PRECISION);
            return new PermutationResult(n, observed, counters[1], p, true,
                    "Permutacao exata: " + counters[1] + " arranjo(s) da matriz " + n + "x" + n + ".");
        }

        if (seedHex == null || seedHex.isBlank()) {
            throw new IllegalArgumentException(
                    "a matrix larger than " + EXACT_LIMIT + " needs the revealed seed, so that "
                            + "the sampled permutations are reproducible");
        }
        SeedStream stream = new SeedStream(
                io.github.rogeriocisi.airlab.support.Hashes.fromHex(seedHex), "permutation-test");
        long atLeastObserved = 0;
        List<Integer> assignment = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            assignment.add(i);
        }
        for (int draw = 0; draw < SAMPLED_PERMUTATIONS; draw++) {
            stream.shuffle(assignment);
            double sum = 0.0;
            for (int sitter = 0; sitter < n; sitter++) {
                sum += scores[sitter][assignment.get(sitter)];
            }
            if (sum >= observed - 1e-9) {
                atLeastObserved++;
            }
        }
        BigDecimal p = BigDecimal.valueOf(atLeastObserved)
                .divide(BigDecimal.valueOf(SAMPLED_PERMUTATIONS), PRECISION);
        return new PermutationResult(n, observed, SAMPLED_PERMUTATIONS, p, false,
                "Permutacao amostrada a partir da semente revelada: " + SAMPLED_PERMUTATIONS
                        + " arranjos de uma matriz " + n + "x" + n + ".");
    }

    /** Depth-first enumeration of the assignments of readings to sitters. */
    private static void enumerate(double[][] scores, int[] assignment, boolean[] used, int sitter,
                                  double observed, long[] counters) {
        int n = scores.length;
        if (sitter == n) {
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                sum += scores[i][assignment[i]];
            }
            counters[1]++;
            if (sum >= observed - 1e-9) {
                counters[0]++;
            }
            return;
        }
        for (int reading = 0; reading < n; reading++) {
            if (used[reading]) {
                continue;
            }
            used[reading] = true;
            assignment[sitter] = reading;
            enumerate(scores, assignment, used, sitter + 1, observed, counters);
            used[reading] = false;
        }
    }

    private static int validate(double[][] scores) {
        if (scores == null || scores.length == 0) {
            throw new IllegalArgumentException("the matrix cannot be empty");
        }
        int n = scores.length;
        for (double[] row : scores) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("the Pratt and Birge matrix must be square");
            }
        }
        return n;
    }
}
