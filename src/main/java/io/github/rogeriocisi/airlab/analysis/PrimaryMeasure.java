package io.github.rogeriocisi.airlab.analysis;

/**
 * Which quantity the confirmatory analysis tests (Epic E7, SPEC section 5).
 *
 * <p>The choice has a large consequence for how many pairs the study needs, which is why it
 * is preregistered and why {@link PowerCheck} computes the requirement from it. The paired
 * measures compare the target reading against its decoy <em>within</em> the same pair, so the
 * unit stays independent while the measurement stays continuous; the forced choice collapses
 * the same data into one bit and is the least sensitive of the three.
 *
 * <p>Beischel and Schwartz anticipated this in 2007: they expected the forced choice to be
 * "a less sensitive indicator" than the paired ratings, and their own numbers bore it out
 * (global ratings P = .007, forced choice P = .01). The 2021 meta-analysis puts the field's
 * effect at 6 to 14 percentage points above chance, which makes the difference decisive:
 * see the table in {@code docs/PLAN.md}.
 */
public enum PrimaryMeasure {

    /**
     * Within-pair difference in the percentage of items scored as fitting, target minus
     * decoy, tested with the exact Wilcoxon signed-rank test. The default.
     */
    PAIRED_ITEM_ACCURACY,

    /**
     * Within-pair difference in the global reading score (0 to 6), target minus decoy,
     * tested with the exact Wilcoxon signed-rank test.
     */
    PAIRED_GLOBAL_SCORE,

    /**
     * The automated structured reading, P4 (SPEC E13): per pair,
     * {@code d = [s(R_A, A) - s(R_A, B)] + [s(R_B, B) - s(R_B, A)]}, where {@code s} counts the
     * comparable fields on which a reading matches a sitter's answers, tested with the exact
     * Wilcoxon signed-rank test over the pairs. Same power calculation as the other paired
     * measures.
     */
    PAIRED_FIELD_MATCHES,

    /**
     * Proportion of correct forced choices against 0.5, tested with the exact binomial
     * test. Valid, preregistrable and far less powerful; keep it as a secondary measure
     * unless there is a reason to do otherwise.
     */
    FORCED_CHOICE
}
