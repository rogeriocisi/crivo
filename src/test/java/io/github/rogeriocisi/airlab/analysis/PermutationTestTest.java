package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Pratt and Birge permutation analysis (Epic E7).
 *
 * <p>Reference values computed independently by enumerating the n! arrangements.
 */
class PermutationTestTest {

    @Test
    @DisplayName("diagonal maxima numa matriz 3x3 da o menor p possivel, 1/6")
    void a_perfect_diagonal_gives_the_smallest_attainable_p() {
        double[][] scores = {
                {9, 1, 1},
                {1, 9, 1},
                {1, 1, 9}};

        PermutationResult result = PermutationTest.diagonalExceedsChance(scores, null);

        assertThat(result.exact()).isTrue();
        assertThat(result.permutations()).isEqualTo(6);
        assertThat(result.observedDiagonalSum()).isEqualTo(27.0);
        assertThat(result.pValue().doubleValue()).isCloseTo(1.0 / 6, Offset.offset(1e-12));
    }

    @Test
    @DisplayName("matriz plana nao distingue nada: p = 1")
    void a_flat_matrix_says_nothing() {
        double[][] scores = {
                {5, 5, 5},
                {5, 5, 5},
                {5, 5, 5}};

        assertThat(PermutationTest.diagonalExceedsChance(scores, null).pValue().doubleValue())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("quando os sitters preferem as leituras dos outros, p = 1")
    void sitters_preferring_other_readings_gives_p_one() {
        double[][] scores = {
                {1, 9, 9},
                {9, 1, 9},
                {9, 9, 1}};

        assertThat(PermutationTest.diagonalExceedsChance(scores, null).pValue().doubleValue())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("com as medias publicadas por O'Keeffe e Wiseman, o resultado e nulo")
    void the_published_null_result_stays_null() {
        // Medias dos quatro primeiros sitters e leituras da Tabela 4 de O'Keeffe e Wiseman
        // (2005): a diagonal nao se destaca, e foi esta a conclusao do estudo.
        double[][] scores = {
                {3.33, 3.72, 1.52, 3.67},
                {2.88, 4.15, 2.25, 2.58},
                {2.77, 3.42, 1.55, 2.92},
                {2.67, 3.75, 2.27, 3.11}};

        PermutationResult result = PermutationTest.diagonalExceedsChance(scores, null);

        assertThat(result.permutations()).isEqualTo(24);
        assertThat(result.pValue().doubleValue()).isCloseTo(0.25, Offset.offset(1e-12));
        assertThat(result.pValue().doubleValue()).isGreaterThan(0.05);
    }

    @Test
    @DisplayName("matriz nao quadrada e recusada")
    void a_non_square_matrix_is_refused() {
        assertThatThrownBy(() -> PermutationTest.diagonalExceedsChance(
                new double[][]{{1, 2, 3}, {4, 5, 6}}, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("square");
    }

    @Test
    @DisplayName("acima do limite exato o teste exige a semente, para ser reproduzivel")
    void a_large_matrix_requires_the_seed() {
        double[][] scores = new double[9][9];
        for (int i = 0; i < 9; i++) {
            scores[i][i] = 1.0;
        }

        assertThatThrownBy(() -> PermutationTest.diagonalExceedsChance(scores, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed");

        String seed = "a".repeat(64);
        PermutationResult sampled = PermutationTest.diagonalExceedsChance(scores, seed);
        assertThat(sampled.exact()).isFalse();
        assertThat(sampled.method()).contains("semente revelada");
        // Mesma semente, mesmo numero: a amostragem tambem e reproduzivel (P5).
        assertThat(PermutationTest.diagonalExceedsChance(scores, seed).pValue())
                .isEqualTo(sampled.pValue());
    }
}
