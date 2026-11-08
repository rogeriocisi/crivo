package io.github.rogeriocisi.airlab.analysis;

/**
 * How an analysis is labelled in every report (Constitution P2).
 *
 * <p>The label is decided by {@link ConfirmatoryAnalysisPolicy}, from the frozen
 * preregistration and the data actually collected, not by whoever runs the analysis.
 */
public enum AnalysisKind {

    /** Preregistered, run exactly as declared. At most one per study. */
    CONFIRMATORY,

    /** Everything else, including a preregistered analysis run off its declared terms. */
    EXPLORATORY
}
