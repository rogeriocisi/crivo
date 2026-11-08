package io.github.rogeriocisi.airlab.study;

/**
 * When data collection stops, decided before it starts.
 *
 * <p>This exists because of a specific criticism of the field: studies have been faulted for
 * "optional stopping", that is, stopping collection once the numbers look good. A declared
 * rule turns that from a matter of discipline into something the system can check when it
 * decides whether an analysis may be labelled confirmatory
 * (see {@code analysis.ConfirmatoryAnalysisPolicy}).
 */
public enum StoppingRule {

    /**
     * Collection stops at exactly the preregistered number of independent units, with no
     * interim analysis. The only rule v1 supports, and the only one for which the exact
     * binomial test of the primary analysis is valid as written.
     */
    FIXED_SAMPLE_SIZE
}
