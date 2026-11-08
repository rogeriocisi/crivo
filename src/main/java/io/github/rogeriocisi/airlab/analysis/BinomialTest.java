package io.github.rogeriocisi.airlab.analysis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Exact one sided binomial test: the primary analysis of the paired protocol, comparing the
 * proportion of correct forced choices against 0.5 (Epic E7, SPEC section 5).
 *
 * <p>Computed in exact rational arithmetic and only then rounded, instead of accumulating
 * floating point error over a sum of binomial terms. Reproducibility is a requirement here
 * (P7, P9): the same input must give the same number in the application and in the R script
 * of the verification package.
 */
public final class BinomialTest {

    private static final MathContext PRECISION = new MathContext(40, RoundingMode.HALF_EVEN);

    private BinomialTest() {
    }

    /**
     * Probability of observing {@code hits} or more successes out of {@code trials} when
     * each trial succeeds with probability 1/2.
     *
     * @throws IllegalArgumentException if the counts are not a possible observation
     */
    public static BigDecimal pValueGreaterOrEqual(int hits, int trials) {
        if (trials < 0 || hits < 0 || hits > trials) {
            throw new IllegalArgumentException("hits must be between 0 and trials");
        }
        // sum of C(trials, k) for k >= hits, divided by 2^trials
        BigInteger numerator = BigInteger.ZERO;
        for (int k = hits; k <= trials; k++) {
            numerator = numerator.add(binomial(trials, k));
        }
        BigDecimal denominator = new BigDecimal(BigInteger.TWO.pow(trials));
        return new BigDecimal(numerator).divide(denominator, PRECISION);
    }

    /** The primary analysis as it is reported: proportion, p value and the decision rule. */
    public static PrimaryAnalysis primary(int hits, int trials, BigDecimal alpha) {
        BigDecimal proportion = trials == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(hits).divide(BigDecimal.valueOf(trials), PRECISION);
        BigDecimal pValue = pValueGreaterOrEqual(hits, trials);
        return new PrimaryAnalysis(hits, trials, proportion, pValue, alpha,
                pValue.compareTo(alpha) <= 0);
    }

    private static BigInteger binomial(int n, int k) {
        BigInteger result = BigInteger.ONE;
        int kk = Math.min(k, n - k);
        for (int i = 1; i <= kk; i++) {
            result = result.multiply(BigInteger.valueOf(n - kk + i)).divide(BigInteger.valueOf(i));
        }
        return result;
    }
}
