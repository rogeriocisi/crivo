package io.github.rogeriocisi.airlab.analysis;

import java.math.BigDecimal;

/**
 * Result of the Pratt and Birge permutation test.
 *
 * @param sitters      size of the square matrix
 * @param observedDiagonalSum sum of the scores sitters gave their own target readings
 * @param permutations how many arrangements were considered
 * @param exact        true when every arrangement was enumerated; false when sampled from
 *                     the revealed seed
 * @param method       how the number was produced, for the report and the audit trail
 */
public record PermutationResult(int sitters,
                                double observedDiagonalSum,
                                long permutations,
                                BigDecimal pValue,
                                boolean exact,
                                String method) {
}
