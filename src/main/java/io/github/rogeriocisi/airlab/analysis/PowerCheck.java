package io.github.rogeriocisi.airlab.analysis;

/**
 * How many independent pairs the declared primary measure needs, given the effect the study
 * says it expects (Epic E7, SPEC section 9, question 1).
 *
 * <p>This exists because the planned sample size of this protocol was computed for a 65%
 * hit rate, while the 2021 meta-analysis of the field puts the effect at 6 to 14 percentage
 * points above chance. At 58% and with the forced choice as the primary measure, 70 pairs
 * give about 32% power: the study would most likely produce a null result that says nothing
 * either way, which is the worst outcome for a protocol whose purpose is a defensible answer.
 *
 * <p>So the requirement is computed and written into the frozen preregistration document.
 * The platform does not refuse an underpowered study: a pilot is legitimate, and refusing
 * would just move the design off the record. It states the number, and the report repeats it.
 *
 * <p>Standard formulas, one-sided, no external dependency:
 * <ul>
 *   <li>forced choice: {@code n = (z_alpha*sqrt(p0*q0) + z_beta*sqrt(p1*q1))^2 / (p1-p0)^2}</li>
 *   <li>paired measures: {@code n = (z_alpha + z_beta)^2 / d^2}, with 15% added for the
 *       Wilcoxon's efficiency relative to the paired t test</li>
 * </ul>
 */
public final class PowerCheck {

    /** z for the one-sided 5% level. */
    private static final double Z_ALPHA_05 = 1.6448536269514722;

    /** z for 80% power. */
    private static final double Z_BETA_80 = 0.8416212335729143;

    /** Asymptotic relative efficiency allowance for the rank test. */
    private static final double WILCOXON_ALLOWANCE = 1.15;

    private PowerCheck() {
    }

    /**
     * @param measure            the preregistered primary measure
     * @param expectedEffect     for {@link PrimaryMeasure#FORCED_CHOICE}, the expected hit
     *                           rate (for example 0.58); for the paired measures, the
     *                           expected standardised difference d (for example 0.50)
     * @param declaredPairs      the preregistered target number of independent pairs
     */
    public static PowerAssessment assess(PrimaryMeasure measure, double expectedEffect,
                                         int declaredPairs) {
        int required = requiredPairs(measure, expectedEffect);
        boolean adequate = declaredPairs >= required;
        String detail = adequate
                ? "Tamanho-alvo de " + declaredPairs + " par(es) atende o minimo de " + required
                        + " para detectar o efeito declarado com alfa 0,05 unilateral e poder 0,80."
                : "Tamanho-alvo de " + declaredPairs + " par(es) esta abaixo do minimo de "
                        + required + " para o efeito declarado (alfa 0,05 unilateral, poder 0,80). "
                        + "O estudo e viavel como piloto, mas um resultado nulo nao distinguira "
                        + "ausencia de efeito de falta de poder, e o relatorio dira isso.";
        return new PowerAssessment(measure, expectedEffect, declaredPairs, required, adequate, detail);
    }

    public static int requiredPairs(PrimaryMeasure measure, double expectedEffect) {
        return switch (measure) {
            case FORCED_CHOICE -> requiredForForcedChoice(expectedEffect);
            case PAIRED_ITEM_ACCURACY, PAIRED_GLOBAL_SCORE, PAIRED_FIELD_MATCHES -> requiredForPaired(expectedEffect);
        };
    }

    private static int requiredForForcedChoice(double expectedHitRate) {
        if (expectedHitRate <= 0.5 || expectedHitRate >= 1.0) {
            throw new IllegalArgumentException(
                    "the expected hit rate must be above 0.5 and below 1 for a one-sided test");
        }
        double p0 = 0.5;
        double numerator = Z_ALPHA_05 * Math.sqrt(p0 * (1 - p0))
                + Z_BETA_80 * Math.sqrt(expectedHitRate * (1 - expectedHitRate));
        double n = (numerator * numerator) / Math.pow(expectedHitRate - p0, 2);
        return (int) Math.ceil(n);
    }

    private static int requiredForPaired(double expectedD) {
        if (expectedD <= 0) {
            throw new IllegalArgumentException("the expected effect size must be positive");
        }
        double n = Math.pow(Z_ALPHA_05 + Z_BETA_80, 2) / (expectedD * expectedD);
        return (int) Math.ceil(n * WILCOXON_ALLOWANCE);
    }
}
