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
        return pValueGreaterOrEqual(hits, trials, 1, 2);
    }

    /**
     * Same tail probability for a chance level of {@code chanceNumerator / chanceDenominator},
     * computed in exact integer arithmetic and only then rounded.
     *
     * <p>The forced-choice protocols of the catalog have a declared chance level that is not
     * one half: four alternatives means 1/4 (SPEC section 1.1). The chance level is a
     * parameter of the protocol, never something estimated from the data.
     *
     * @throws IllegalArgumentException if the counts are impossible or the chance level is
     *                                  not a probability strictly between 0 and 1
     */
    public static BigDecimal pValueGreaterOrEqual(int hits, int trials,
                                                  int chanceNumerator, int chanceDenominator) {
        if (trials < 0 || hits < 0 || hits > trials) {
            throw new IllegalArgumentException("hits must be between 0 and trials");
        }
        if (chanceNumerator <= 0 || chanceDenominator <= chanceNumerator) {
            throw new IllegalArgumentException("the chance level must be between 0 and 1");
        }
        // sum of C(n,k) * a^k * (b-a)^(n-k) for k >= hits, divided by b^n
        BigInteger a = BigInteger.valueOf(chanceNumerator);
        BigInteger complement = BigInteger.valueOf(chanceDenominator - chanceNumerator);
        BigInteger numerator = BigInteger.ZERO;
        for (int k = hits; k <= trials; k++) {
            numerator = numerator.add(binomial(trials, k)
                    .multiply(a.pow(k))
                    .multiply(complement.pow(trials - k)));
        }
        BigDecimal denominator = new BigDecimal(BigInteger.valueOf(chanceDenominator).pow(trials));
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
