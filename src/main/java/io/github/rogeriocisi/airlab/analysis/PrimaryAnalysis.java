package io.github.rogeriocisi.airlab.analysis;

import java.math.BigDecimal;

/**
 * Result of the preregistered primary analysis.
 *
 * <p>Neutral by construction (P3): a null result and a positive result are the same record,
 * rendered by the same report. {@code belowAlpha} is a statement about the test, not a verdict
 * about the phenomenon, and the report says so in those words.
 */
public record PrimaryAnalysis(int hits,
                              int trials,
                              BigDecimal proportion,
                              BigDecimal pValue,
                              BigDecimal alpha,
                              boolean belowAlpha) {
}
