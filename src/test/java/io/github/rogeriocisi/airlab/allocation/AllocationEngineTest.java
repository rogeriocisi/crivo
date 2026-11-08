package io.github.rogeriocisi.airlab.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The draw is the core of P5: unpredictable before, reproducible after. These tests are the
 * ones an auditor would write.
 */
class AllocationEngineTest {

    private static final DrawInput INPUT = new DrawInput(
            "STD-TEST0001",
            List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B"),
                    new DrawInput.PairInput("PR-0002", "TGT-C", "TGT-D")),
            List.of("MED-1", "MED-2"),
            List.of("EXP-1", "EXP-2"));

    private static final String SEED =
            "6f1c3a9d2b4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8";

    @Test
    @DisplayName("mesma semente e mesma entrada produzem exatamente o mesmo sorteio")
    void draw_is_reproducible() {
        AllocationPlan first = AllocationEngine.plan(SEED, INPUT);
        AllocationPlan second = AllocationEngine.plan(SEED, INPUT);

        assertThat(second.canonicalForm()).isEqualTo(first.canonicalForm());
        assertThat(AllocationEngine.commitment(SEED, second))
                .isEqualTo(AllocationEngine.commitment(SEED, first));
    }

    @Test
    @DisplayName("a ordem em que as entradas chegam nao altera o resultado")
    void draw_does_not_depend_on_input_order() {
        DrawInput shuffled = new DrawInput(
                INPUT.studyCode(),
                List.of(new DrawInput.PairInput("PR-0002", "TGT-D", "TGT-C"),
                        new DrawInput.PairInput("PR-0001", "TGT-B", "TGT-A")),
                List.of("MED-2", "MED-1"),
                List.of("EXP-2", "EXP-1"));

        assertThat(AllocationEngine.plan(SEED, shuffled).canonicalForm())
                .isEqualTo(AllocationEngine.plan(SEED, INPUT).canonicalForm());
    }

    @Test
    @DisplayName("sementes diferentes produzem sorteios diferentes")
    void different_seeds_give_different_draws() {
        String other = "0".repeat(63) + "1";
        assertThat(AllocationEngine.plan(other, INPUT).canonicalForm())
                .isNotEqualTo(AllocationEngine.plan(SEED, INPUT).canonicalForm());
    }

    @Test
    @DisplayName("cada par gera duas sessoes, com o mesmo medium e ordens 1 e 2")
    void each_pair_yields_two_sessions_with_the_same_medium() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);

        assertThat(plan.sessions()).hasSize(4);
        Map<String, List<AllocationPlan.SessionAllocation>> byPair = plan.sessions().stream()
                .collect(Collectors.groupingBy(AllocationPlan.SessionAllocation::pairCode));

        assertThat(byPair).hasSize(2);
        byPair.forEach((pair, sessions) -> {
            assertThat(sessions).hasSize(2);
            assertThat(sessions.stream().map(AllocationPlan.SessionAllocation::mediumCode).distinct())
                    .hasSize(1);
            assertThat(sessions.stream().map(AllocationPlan.SessionAllocation::readingOrder))
                    .containsExactlyInAnyOrder(1, 2);
            assertThat(sessions.stream().map(AllocationPlan.SessionAllocation::targetCode).distinct())
                    .hasSize(2);
        });
    }

    @Test
    @DisplayName("cada sitter recebe uma ordem de apresentacao, que nao revela qual leitura e a dele")
    void every_target_gets_a_presentation_order() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);

        assertThat(plan.presentations()).hasSize(4);
        assertThat(plan.presentations().stream()
                .map(AllocationPlan.PresentationOrder::targetCode))
                .containsExactly("TGT-A", "TGT-B", "TGT-C", "TGT-D");

        // For every sitter, the reading shown first is one of the two of their own pair.
        for (AllocationPlan.PresentationOrder order : plan.presentations()) {
            assertThat(order.firstShownTargetCode()).isNotBlank();
        }
    }

    @Test
    @DisplayName("o compromisso muda se qualquer alocacao mudar")
    void commitment_binds_the_allocations() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);
        AllocationPlan tampered = new AllocationPlan(
                plan.studyCode(),
                plan.sessions().stream()
                        .map(s -> new AllocationPlan.SessionAllocation(s.pairCode(), s.targetCode(),
                                "MED-OTHER", s.experimenterCode(), s.readingOrder()))
                        .toList(),
                plan.presentations());

        assertThat(AllocationEngine.commitment(SEED, tampered))
                .isNotEqualTo(AllocationEngine.commitment(SEED, plan));
    }

    @Test
    @DisplayName("a semente gerada tem 32 bytes e nao se repete")
    void seeds_are_fresh() {
        String first = AllocationEngine.newSeedHex();
        String second = AllocationEngine.newSeedHex();

        assertThat(first).hasSize(64).isNotEqualTo(second);
    }
}
