package io.github.rogeriocisi.airlab.study;

/**
 * What counts as one independent observation in the primary analysis.
 *
 * <p>The paired protocol creates a dependence that is easy to miss: the two sitters of a
 * pair judge the same two readings, mirrored. Counting both of their forced choices as
 * independent trials inflates the effective sample size and makes the binomial test
 * anti-conservative.
 */
public enum AnalysisUnit {

    /**
     * One observation per pair: the forced choice of the sitter the draw designated for the
     * confirmatory analysis (see {@code AllocationPlan.ConfirmatoryUnit}). Independent by
     * construction, at the cost of halving the number of observations.
     *
     * <p>The default and the only unit v1 accepts for a confirmatory analysis.
     */
    PAIR,

    /**
     * One observation per forced choice, both sitters of each pair included. Valid only with
     * a method that accounts for clustering within the pair, which v1 does not implement, so
     * an analysis declared on this unit is reported as exploratory.
     */
    FORCED_CHOICE
}
