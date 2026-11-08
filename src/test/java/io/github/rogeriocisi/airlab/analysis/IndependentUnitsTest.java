package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SPEC section 9, question 1: the two sitters of a pair are not independent observations.
 * Only the sitter the draw designated counts in the confirmatory analysis.
 */
class IndependentUnitsTest {

    private static final Map<String, String> DESIGNATED = Map.of(
            "PR-1", "TGT-A",
            "PR-2", "TGT-D");

    @Test
    @DisplayName("conta uma escolha por par, a do sitter sorteado")
    void counts_one_choice_per_pair() {
        List<IndependentUnits.Choice> choices = List.of(
                new IndependentUnits.Choice("PR-1", "TGT-A", true),
                new IndependentUnits.Choice("PR-1", "TGT-B", false),
                new IndependentUnits.Choice("PR-2", "TGT-C", true),
                new IndependentUnits.Choice("PR-2", "TGT-D", false));

        IndependentUnits.Counts counts = IndependentUnits.confirmatoryCounts(choices, DESIGNATED);

        assertThat(counts.trials()).isEqualTo(2);
        assertThat(counts.hits()).isEqualTo(1);
    }

    @Test
    @DisplayName("a escolha do outro sitter do par nao entra, mesmo sendo um acerto")
    void the_other_sitter_never_enters_the_confirmatory_count() {
        List<IndependentUnits.Choice> choices = List.of(
                new IndependentUnits.Choice("PR-1", "TGT-A", false),
                new IndependentUnits.Choice("PR-1", "TGT-B", true),
                new IndependentUnits.Choice("PR-2", "TGT-D", false),
                new IndependentUnits.Choice("PR-2", "TGT-C", true));

        IndependentUnits.Counts counts = IndependentUnits.confirmatoryCounts(choices, DESIGNATED);

        assertThat(counts.trials()).isEqualTo(2);
        assertThat(counts.hits()).isZero();
    }

    @Test
    @DisplayName("par sem escolha do sitter sorteado interrompe, em vez de encolher a amostra")
    void a_missing_designated_choice_is_refused() {
        List<IndependentUnits.Choice> choices = List.of(
                new IndependentUnits.Choice("PR-1", "TGT-A", true),
                new IndependentUnits.Choice("PR-2", "TGT-C", true));

        assertThatThrownBy(() -> IndependentUnits.confirmatoryCounts(choices, DESIGNATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PR-2");
    }

    @Test
    @DisplayName("escolha duplicada do sitter sorteado e recusada")
    void a_duplicated_designated_choice_is_refused() {
        List<IndependentUnits.Choice> choices = List.of(
                new IndependentUnits.Choice("PR-1", "TGT-A", true),
                new IndependentUnits.Choice("PR-1", "TGT-A", false),
                new IndependentUnits.Choice("PR-2", "TGT-D", true));

        assertThatThrownBy(() -> IndependentUnits.confirmatoryCounts(choices, DESIGNATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than one");
    }

    @Test
    @DisplayName("escolha de um par que nao foi sorteado e recusada")
    void a_choice_from_an_undrawn_pair_is_refused() {
        List<IndependentUnits.Choice> choices = List.of(
                new IndependentUnits.Choice("PR-9", "TGT-Z", true));

        assertThatThrownBy(() -> IndependentUnits.confirmatoryCounts(choices, DESIGNATED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PR-9");
    }

    @Test
    @DisplayName("a contagem nao depende da ordem em que as escolhas chegam")
    void the_count_does_not_depend_on_input_order() {
        List<IndependentUnits.Choice> ascending = List.of(
                new IndependentUnits.Choice("PR-1", "TGT-A", true),
                new IndependentUnits.Choice("PR-2", "TGT-D", true));
        List<IndependentUnits.Choice> descending = List.of(
                new IndependentUnits.Choice("PR-2", "TGT-D", true),
                new IndependentUnits.Choice("PR-1", "TGT-A", true));

        assertThat(IndependentUnits.confirmatoryCounts(descending, DESIGNATED))
                .isEqualTo(IndependentUnits.confirmatoryCounts(ascending, DESIGNATED));
    }
}
