package io.github.rogeriocisi.airlab.trials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Constitution P5 applied to the self-service tests: the target of a trial must be
 * reproducible by a third party from the revealed seed, on another machine.
 */
class TargetDerivationTest {

    private static final String SEED =
            "6f1c3a9d2b4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8";
    private static final String OTHER_SEED = "0".repeat(63) + "1";

    @Test
    @DisplayName("a mesma semente e a mesma tentativa dao sempre o mesmo alvo")
    void derivation_is_reproducible() {
        for (int trial = 1; trial <= 24; trial++) {
            int first = TargetDerivation.targetIndex(SEED, trial, 4);
            int second = TargetDerivation.targetIndex(SEED, trial, 4);
            assertThat(second).isEqualTo(first);
        }
    }

    @Test
    @DisplayName("o alvo fica dentro do numero de alternativas")
    void target_is_within_range() {
        for (int trial = 1; trial <= 50; trial++) {
            assertThat(TargetDerivation.targetIndex(SEED, trial, 4)).isBetween(0, 3);
        }
    }

    @Test
    @DisplayName("sementes diferentes dao sequencias de alvos diferentes")
    void different_seeds_give_different_targets() {
        boolean anyDifferent = false;
        for (int trial = 1; trial <= 24; trial++) {
            if (TargetDerivation.targetIndex(SEED, trial, 4)
                    != TargetDerivation.targetIndex(OTHER_SEED, trial, 4)) {
                anyDifferent = true;
                break;
            }
        }
        assertThat(anyDifferent).isTrue();
    }

    @Test
    @DisplayName("as quatro alternativas aparecem como alvo ao longo de uma sessao")
    void every_alternative_shows_up() {
        Set<Integer> seen = new HashSet<>();
        for (int trial = 1; trial <= 48; trial++) {
            seen.add(TargetDerivation.targetIndex(SEED, trial, 4));
        }
        assertThat(seen).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    @DisplayName("as candidatas da visao remota sao quatro figuras distintas do banco")
    void candidates_are_four_distinct_figures() {
        List<Integer> candidates = TargetDerivation.candidatePoolIndices(
                SEED, 1, TargetPool.size(), 4);

        assertThat(candidates).hasSize(4).doesNotHaveDuplicates();
        candidates.forEach(index -> assertThat(index).isBetween(0, TargetPool.size() - 1));
    }

    @Test
    @DisplayName("as candidatas tambem sao reproduziveis pela semente")
    void candidates_are_reproducible() {
        assertThat(TargetDerivation.candidatePoolIndices(SEED, 3, TargetPool.size(), 4))
                .isEqualTo(TargetDerivation.candidatePoolIndices(SEED, 3, TargetPool.size(), 4));
    }

    @Test
    @DisplayName("tentativas diferentes mostram conjuntos de candidatas diferentes")
    void different_trials_draw_different_candidates() {
        assertThat(TargetDerivation.candidatePoolIndices(SEED, 1, TargetPool.size(), 4))
                .isNotEqualTo(TargetDerivation.candidatePoolIndices(SEED, 2, TargetPool.size(), 4));
    }

    @Test
    @DisplayName("banco menor que o numero de candidatas e recusado, nao ajustado")
    void a_pool_smaller_than_the_trial_is_refused() {
        assertThatThrownBy(() -> TargetDerivation.candidatePoolIndices(SEED, 1, 3, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fewer");
    }

    @Test
    @DisplayName("indice de tentativa comeca em 1")
    void trial_index_starts_at_one() {
        assertThatThrownBy(() -> TargetDerivation.targetIndex(SEED, 0, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
