package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Constitution P9: the paired primary measure is validated against exact reference values.
 *
 * <p>Each expected p value below is the exact rational {@code #{sign assignments with
 * W+ >= observed} / 2^n}, computed independently; they agree with
 * {@code wilcox.test(..., alternative = "greater", exact = TRUE)} in R.
 */
class PairedPrimaryMeasureTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "todas positivas n=5,      0.03125,    1;2;3;4;5",
            "todas negativas n=5,      1.0,        -1;-2;-3;-4;-5",
            "mistura n=6,              0.078125,   2;-1;3;4;-0.5;5",
            "com zeros descartados,    0.1875,     0;1.5;-2;0;3;4",
            "empates com mid-ranks,    0.25,       1;1;-1;2",
            "oito pares,               0.02734375, 1;2;0.5;3;-1.5;2.5;1;4"
    })
    @DisplayName("o p-valor exato do Wilcoxon confere com os valores de referencia")
    void matches_reference_values(String name, double expected, String rawDifferences) {
        List<Double> differences = java.util.Arrays.stream(rawDifferences.split(";"))
                .map(Double::parseDouble)
                .toList();

        WilcoxonResult result = WilcoxonSignedRank.greaterThanZero(differences);

        assertThat(result.exact()).isTrue();
        assertThat(result.pValue().doubleValue()).isCloseTo(expected, Offset.offset(1e-12));
    }

    @Test
    @DisplayName("pares empatados sao descartados pelo teste e reportados, nao escondidos")
    void dropped_zeros_are_reported() {
        WilcoxonResult result = WilcoxonSignedRank.greaterThanZero(
                List.of(0.0, 1.5, -2.0, 0.0, 3.0, 4.0));

        assertThat(result.pairs()).isEqualTo(4);
        assertThat(result.droppedZeros()).isEqualTo(2);
        assertThat(result.method()).contains("exato");
    }

    @Test
    @DisplayName("sem nenhuma diferenca o teste nao inventa informacao")
    void all_ties_yield_no_information() {
        WilcoxonResult result = WilcoxonSignedRank.greaterThanZero(List.of(0.0, 0.0, 0.0));

        assertThat(result.pairs()).isZero();
        assertThat(result.pValue().doubleValue()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("a comparacao pareada usa alvo menos chamariz, uma observacao por par")
    void paired_comparison_uses_one_observation_per_pair() {
        WilcoxonResult result = PairedComparison.targetExceedsDecoy(List.of(
                new PairedComparison.PairScore("PR-1", 5.0, 2.0),
                new PairedComparison.PairScore("PR-2", 4.0, 1.0),
                new PairedComparison.PairScore("PR-3", 3.0, 2.0),
                new PairedComparison.PairScore("PR-4", 6.0, 1.0),
                new PairedComparison.PairScore("PR-5", 5.0, 0.0)));

        assertThat(result.pairs()).isEqualTo(5);
        // All five differences positive: the smallest p the test can give with n = 5.
        assertThat(result.pValue().doubleValue()).isCloseTo(0.03125, Offset.offset(1e-12));
    }

    @Test
    @DisplayName("a ordem em que os pares chegam nao altera o resultado")
    void order_of_pairs_does_not_matter() {
        List<PairedComparison.PairScore> ascending = List.of(
                new PairedComparison.PairScore("PR-1", 5.0, 2.0),
                new PairedComparison.PairScore("PR-2", 1.0, 4.0));
        List<PairedComparison.PairScore> descending = List.of(
                new PairedComparison.PairScore("PR-2", 1.0, 4.0),
                new PairedComparison.PairScore("PR-1", 5.0, 2.0));

        assertThat(PairedComparison.targetExceedsDecoy(descending).pValue())
                .isEqualTo(PairedComparison.targetExceedsDecoy(ascending).pValue());
    }

    @Test
    @DisplayName("par duplicado e recusado, em vez de dobrar de peso")
    void a_duplicated_pair_is_refused() {
        assertThatThrownBy(() -> PairedComparison.targetExceedsDecoy(List.of(
                new PairedComparison.PairScore("PR-1", 5.0, 2.0),
                new PairedComparison.PairScore("PR-1", 4.0, 3.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PR-1");
    }
}
