package io.github.rogeriocisi.airlab.analysis;

import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.SeedStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The permutation test of the P4 (SPEC E13, phase D): the readings are reassigned to the targets
 * only <em>within</em> the pairing strata, because a reading made for a woman who died at 80 is
 * not exchangeable with one made for a man who died at 30 — the first name and the strata already
 * separate them, and permuting across strata would reward the leak the pairing exists to cancel.
 *
 * <p>Same question as {@link PermutationTest}: how unusual is the observed diagonal sum among the
 * sums of every allowed assignment? Exact by enumeration while the number of allowed assignments
 * (the product of the factorials of the stratum sizes) stays within {@value #EXACT_LIMIT}; above
 * that, {@value #SAMPLED_PERMUTATIONS} assignments drawn from the revealed seed, so anyone holding
 * the seed gets the same number (P5, P7).
 */
public final class StratifiedPermutationTest {

    private static final MathContext PRECISION = new MathContext(40, RoundingMode.HALF_EVEN);
    static final long EXACT_LIMIT = 40_320;
    static final int SAMPLED_PERMUTATIONS = 100_000;

    private StratifiedPermutationTest() {
    }

    /**
     * @param scores  square matrix, {@code scores[sitter][reading]}; the diagonal is each sitter's
     *                own target reading
     * @param stratum stratum of each sitter, which is also the stratum of the reading on the same
     *                index
     * @param seedHex the revealed seed, used only when the assignments are too many to enumerate
     */
    public static PermutationResult diagonalExceedsChance(double[][] scores, int[] stratum, String seedHex) {
        int n = scores.length;
        if (n == 0 || stratum.length != n) {
            throw new IllegalArgumentException("a square, non-empty matrix with one stratum per row");
        }
        for (double[] row : scores) {
            if (row.length != n) {
                throw new IllegalArgumentException("the score matrix must be square");
            }
        }
        double observed = 0.0;
        for (int i = 0; i < n; i++) {
            observed += scores[i][i];
        }

        Map<Integer, List<Integer>> strata = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            strata.computeIfAbsent(stratum[i], key -> new ArrayList<>()).add(i);
        }
        long arrangements = 1;
        boolean enumerable = true;
        for (List<Integer> members : strata.values()) {
            for (int k = 2; k <= members.size(); k++) {
                arrangements *= k;
                if (arrangements > EXACT_LIMIT) {
                    enumerable = false;
                    break;
                }
            }
            if (!enumerable) {
                break;
            }
        }

        if (enumerable) {
            long[] counters = new long[2];
            enumerate(scores, stratum, new int[n], new boolean[n], 0, observed, counters);
            BigDecimal p = BigDecimal.valueOf(counters[0]).divide(BigDecimal.valueOf(counters[1]), PRECISION);
            return new PermutationResult(n, observed, counters[1], p, true,
                    "Permutacao exata dentro dos estratos: " + counters[1] + " arranjo(s) de " + n
                            + " leitura(s) em " + strata.size() + " estrato(s).");
        }

        if (seedHex == null || seedHex.isBlank()) {
            throw new IllegalArgumentException("too many arrangements to enumerate: the revealed seed is needed");
        }
        SeedStream stream = new SeedStream(Hashes.fromHex(seedHex), "stratified-permutation-test");
        long atLeastObserved = 0;
        int[] assignment = new int[n];
        for (int draw = 0; draw < SAMPLED_PERMUTATIONS; draw++) {
            for (List<Integer> members : strata.values()) {
                List<Integer> shuffled = new ArrayList<>(members);
                stream.shuffle(shuffled);
                for (int i = 0; i < members.size(); i++) {
                    assignment[members.get(i)] = shuffled.get(i);
                }
            }
            double sum = 0.0;
            for (int sitter = 0; sitter < n; sitter++) {
                sum += scores[sitter][assignment[sitter]];
            }
            if (sum >= observed - 1e-9) {
                atLeastObserved++;
            }
        }
        // The observed assignment counts as one of the arrangements, so p is never zero.
        BigDecimal p = BigDecimal.valueOf(atLeastObserved + 1)
                .divide(BigDecimal.valueOf(SAMPLED_PERMUTATIONS + 1L), PRECISION);
        return new PermutationResult(n, observed, SAMPLED_PERMUTATIONS, p, false,
                "Permutacao amostrada dentro dos estratos: " + SAMPLED_PERMUTATIONS
                        + " arranjo(s) sorteados da semente revelada, dominio stratified-permutation-test.");
    }

    private static void enumerate(double[][] scores, int[] stratum, int[] assignment, boolean[] used, int row,
                                  double observed, long[] counters) {
        int n = scores.length;
        if (row == n) {
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
        for (int column = 0; column < n; column++) {
            if (!used[column] && stratum[column] == stratum[row]) {
                used[column] = true;
                assignment[row] = column;
                enumerate(scores, stratum, assignment, used, row + 1, observed, counters);
                used[column] = false;
            }
        }
    }
}
