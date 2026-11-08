package io.github.rogeriocisi.airlab.analysis;

/**
 * Verdict on whether an analysis may be reported as confirmatory.
 *
 * @param kind          the label the report must use
 * @param justification why, in words that go into the report and into the audit trail. Never
 *                      empty, including when the verdict is favourable: the reader is told on
 *                      what terms the label was granted
 */
public record AnalysisEligibility(AnalysisKind kind, String justification) {

    public boolean confirmatory() {
        return kind == AnalysisKind.CONFIRMATORY;
    }

    static AnalysisEligibility confirmatory(String justification) {
        return new AnalysisEligibility(AnalysisKind.CONFIRMATORY, justification);
    }

    static AnalysisEligibility exploratory(String justification) {
        return new AnalysisEligibility(AnalysisKind.EXPLORATORY, justification);
    }
}
