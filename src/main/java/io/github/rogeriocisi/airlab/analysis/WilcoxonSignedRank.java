package io.github.rogeriocisi.airlab.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One-sided exact Wilcoxon signed-rank test, used for the paired primary measures
 * (target minus decoy within each pair).
 *
 * <p>Exact by enumeration of all 2^n sign assignments up to n = 20, which covers a pilot and
 * most of the pair counts this protocol plans for. Above that the enumeration stops being
 * practical and the test falls back to the normal approximation with a tie correction. The
 * result says which of the two produced the number, because an auditor must be able to tell
 * (P7, P9).
 *
 * <p>Zero differences are dropped and ties receive mid-ranks, as in the standard definition.
 * Dropping zeros reduces n, which the report states rather than hides.
 */
public final class WilcoxonSignedRank {

    private static final MathContext PRECISION = new MathContext(40, RoundingMode.HALF_EVEN);
    private static final int EXACT_LIMIT = 20;

    private WilcoxonSignedRank() {
    }

    /**
     * @param differences target minus decoy, one per pair
     * @return the result of testing "the differences are positive" one-sided
     */
    public static WilcoxonResult greaterThanZero(List<Double> differences) {
        List<Double> nonZero = new ArrayList<>();
        for (Double d : differences) {
            if (d == null) {
                throw new IllegalArgumentException("a pair cannot contribute a null difference");
            }
            if (d != 0.0) {
                nonZero.add(d);
            }
        }
        int droppedZeros = differences.size() - nonZero.size();
        int n = nonZero.size();
        if (n == 0) {
            return new WilcoxonResult(0, droppedZeros, 0.0, BigDecimal.ONE, true,
                    "Nenhuma diferenca diferente de zero: o teste nao tem informacao.");
        }

        double[] ranks = midRanks(nonZero);
        double positiveRankSum = 0.0;
        for (int i = 0; i < n; i++) {
            if (nonZero.get(i) > 0) {
                positiveRankSum += ranks[i];
            }
        }

        if (n <= EXACT_LIMIT) {
            BigDecimal p = exactPValue(ranks, positiveRankSum);
            return new WilcoxonResult(n, droppedZeros, positiveRankSum, p, true,
                    "Wilcoxon exato, unilateral, por enumeracao dos 2^" + n + " sinais.");
        }
        BigDecimal p = approximatePValue(ranks, positiveRankSum, n);
        return new WilcoxonResult(n, droppedZeros, positiveRankSum, p, false,
                "Wilcoxon unilateral pela aproximacao normal com correcao de empates "
                        + "(n = " + n + " acima do limite exato de " + EXACT_LIMIT + ").");
    }

    /**
     * P(W+ >= observed) under the null, by enumerating every assignment of signs to the
     * observed absolute values. Each assignment has probability 2^-n.
     */
    private static BigDecimal exactPValue(double[] ranks, double observed) {
        int n = ranks.length;
        long total = 1L << n;
        long atLeastObserved = 0;
        // A tolerance is needed because mid-ranks are halves and sums are floating point.
        double tolerance = 1e-9;
        for (long mask = 0; mask < total; mask++) {
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1L << i)) != 0) {
                    sum += ranks[i];
                }
            }
            if (sum >= observed - tolerance) {
                atLeastObserved++;
            }
        }
        return BigDecimal.valueOf(atLeastObserved).divide(BigDecimal.valueOf(total), PRECISION);
    }

    private static BigDecimal approximatePValue(double[] ranks, double observed, int n) {
        double mean = n * (n + 1) / 4.0;
        double variance = n * (n + 1) * (2.0 * n + 1) / 24.0 - tieCorrection(ranks);
        if (variance <= 0) {
            return BigDecimal.ONE;
        }
        // Continuity correction of 0.5, as usual for a discrete statistic.
        double z = (observed - mean - 0.5) / Math.sqrt(variance);
        return BigDecimal.valueOf(1.0 - standardNormalCdf(z)).round(PRECISION);
    }

    private static double tieCorrection(double[] ranks) {
        // Sum over tied groups of (t^3 - t) / 48, the standard adjustment.
        double[] sorted = ranks.clone();
        java.util.Arrays.sort(sorted);
        double correction = 0.0;
        int i = 0;
        while (i < sorted.length) {
            int j = i;
            while (j + 1 < sorted.length && sorted[j + 1] == sorted[i]) {
                j++;
            }
            long t = j - i + 1L;
            if (t > 1) {
                correction += (t * t * t - t) / 48.0;
            }
            i = j + 1;
        }
        return correction;
    }

    private static double standardNormalCdf(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    /** Abramowitz and Stegun 7.1.26: enough precision for a reported p value. */
    private static double erf(double x) {
        double sign = x < 0 ? -1.0 : 1.0;
        double ax = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * ax);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
                - 0.284496736) * t + 0.254829592) * t * Math.exp(-ax * ax);
        return sign * y;
    }

    /** Ranks of the absolute differences, with mid-ranks for ties. */
    private static double[] midRanks(List<Double> values) {
        int n = values.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(i -> Math.abs(values.get(i))));

        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            double current = Math.abs(values.get(order.get(i)));
            while (j + 1 < n && Math.abs(values.get(order.get(j + 1))) == current) {
                j++;
            }
            double midRank = (i + j + 2) / 2.0;
            for (int k = i; k <= j; k++) {
                ranks[order.get(k)] = midRank;
            }
            i = j + 1;
        }
        return ranks;
    }
}
