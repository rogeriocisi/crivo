package io.github.rogeriocisi.airlab.analysis;

import java.math.BigDecimal;

/**
 * Result of the paired primary measure.
 *
 * @param pairs        pairs that contributed a non-zero difference
 * @param droppedZeros pairs whose target and decoy scored the same, dropped by the test and
 *                     reported here rather than passed over in silence
 * @param positiveRankSum the W+ statistic
 * @param exact        true when the p value came from full enumeration
 * @param method       how the number was produced, for the report and the audit trail
 */
public record WilcoxonResult(int pairs,
                             int droppedZeros,
                             double positiveRankSum,
                             BigDecimal pValue,
                             boolean exact,
                             String method) {
}
