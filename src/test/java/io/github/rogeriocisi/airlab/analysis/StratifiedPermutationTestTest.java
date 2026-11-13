package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The P4 permutation test, readings permuted only within the pairing strata (SPEC E13, phase D). */
class StratifiedPermutationTestTest {

    private static final String SEED = "6f1c3a9d2b4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8";

    @Test
    @DisplayName("so permuta dentro do estrato: dois estratos de dois dao quatro arranjos")
    void permutes_only_within_strata() {
        double[][] scores = {
                {9, 1, 0, 0},
                {1, 9, 0, 0},
                {0, 0, 9, 1},
                {0, 0, 1, 9}};
        PermutationResult result = StratifiedPermutationTest.diagonalExceedsChance(scores, new int[]{0, 0, 1, 1}, null);

        assertThat(result.exact()).isTrue();
        assertThat(result.permutations()).isEqualTo(4);
        assertThat(result.observedDiagonalSum()).isEqualTo(36.0);
        // Only the identity reaches 36 of the four arrangements.
        assertThat(result.pValue()).isEqualByComparingTo(new BigDecimal("0.25"));
    }

    @Test
    @DisplayName("um estrato so equivale ao teste sem estratos")
    void a_single_stratum_matches_the_unstratified_test() {
        double[][] scores = {
                {5, 2, 1},
                {1, 4, 3},
                {2, 2, 6}};
        PermutationResult stratified = StratifiedPermutationTest.diagonalExceedsChance(scores, new int[]{7, 7, 7}, null);
        PermutationResult plain = PermutationTest.diagonalExceedsChance(scores, null);

        assertThat(stratified.permutations()).isEqualTo(plain.permutations());
        assertThat(stratified.pValue()).isEqualByComparingTo(plain.pValue());
    }

    @Test
    @DisplayName("acima do limite, amostra da semente e da o mesmo numero com a mesma semente")
    void above_the_limit_it_samples_reproducibly_from_the_seed() {
        int n = 9;
        double[][] scores = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                scores[i][j] = i == j ? 3 : (i + j) % 3;
            }
        }
        int[] oneStratum = new int[n];
        PermutationResult first = StratifiedPermutationTest.diagonalExceedsChance(scores, oneStratum, SEED);
        PermutationResult second = StratifiedPermutationTest.diagonalExceedsChance(scores, oneStratum, SEED);

        assertThat(first.exact()).isFalse();
        assertThat(first.pValue()).isEqualByComparingTo(second.pValue());
        assertThat(first.pValue()).isGreaterThan(BigDecimal.ZERO);
        assertThatThrownBy(() -> StratifiedPermutationTest.diagonalExceedsChance(scores, oneStratum, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
