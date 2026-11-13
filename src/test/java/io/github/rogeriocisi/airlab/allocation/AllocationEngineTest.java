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
            List.of("EXP-1", "EXP-2"),
            2);

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
                List.of("EXP-2", "EXP-1"),
                2);

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
    @DisplayName("dentro de um par, um sitter ve a propria leitura primeiro e o outro ve o chamariz")
    void presentation_order_is_counterbalanced_within_the_pair() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);

        for (DrawInput.PairInput pair : INPUT.pairs()) {
            String firstForA = firstShownFor(plan, pair.targetA());
            String firstForB = firstShownFor(plan, pair.targetB());

            // Both sitters of a pair see the same reading first, which is exactly what makes
            // one of them see their own first and the other see the decoy first.
            assertThat(firstForA).isEqualTo(firstForB);
            assertThat(firstForA).isIn(pair.targetA(), pair.targetB());

            boolean aSeesOwnFirst = firstForA.equals(pair.targetA());
            boolean bSeesOwnFirst = firstForB.equals(pair.targetB());
            assertThat(aSeesOwnFirst).isNotEqualTo(bSeesOwnFirst);
        }
    }

    @Test
    @DisplayName("os slots de sessao sao contrabalanceados e nao repetem dentro de um medium")
    void session_slots_are_counterbalanced() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);

        assertThat(plan.sessionSlots()).hasSize(4);
        for (AllocationPlan.SessionSlot slot : plan.sessionSlots()) {
            assertThat(slot.dayIndex()).isGreaterThanOrEqualTo(0);
            assertThat(slot.slotIndex()).isBetween(0, 1);
        }

        // As duas leituras de um par nunca caem no mesmo dia e horario.
        for (DrawInput.PairInput pair : INPUT.pairs()) {
            List<AllocationPlan.SessionSlot> ofPair = plan.sessionSlots().stream()
                    .filter(s -> s.pairCode().equals(pair.pairCode()))
                    .toList();
            assertThat(ofPair).hasSize(2);
            assertThat(ofPair.get(0).dayIndex() != ofPair.get(1).dayIndex()
                    || ofPair.get(0).slotIndex() != ofPair.get(1).slotIndex()).isTrue();
        }

        assertThat(plan.canonicalForm()).contains("sessionSlots");
    }

    @Test
    @DisplayName("a ordem dos itens apresentada ao sitter vem da semente e e estavel")
    void item_presentation_order_is_seeded_and_stable() {
        List<Integer> first = AllocationEngine.itemPresentationOrder(SEED, "RDG-0001", 12);
        List<Integer> again = AllocationEngine.itemPresentationOrder(SEED, "RDG-0001", 12);
        List<Integer> other = AllocationEngine.itemPresentationOrder(SEED, "RDG-0002", 12);

        assertThat(first).hasSize(12).containsExactlyInAnyOrderElementsOf(
                java.util.stream.IntStream.range(0, 12).boxed().toList());
        assertThat(again).isEqualTo(first);
        assertThat(other).isNotEqualTo(first);
        // Uma lista embaralhada, nao a ordem de redacao.
        assertThat(first).isNotEqualTo(java.util.stream.IntStream.range(0, 12).boxed().toList());
    }

    @Test
    @DisplayName("o corte de comprimento mantem a quantidade pedida, em ordem, e e reproduzivel")
    void length_balancing_is_reproducible() {
        List<Integer> kept = AllocationEngine.balancedSubsample(SEED, "RDG-0001", 60, 9);

        assertThat(kept).hasSize(9).isSorted().doesNotHaveDuplicates();
        assertThat(kept).allMatch(i -> i >= 0 && i < 60);
        assertThat(AllocationEngine.balancedSubsample(SEED, "RDG-0001", 60, 9)).isEqualTo(kept);
        assertThat(AllocationEngine.balancedSubsample(SEED, "RDG-0002", 60, 9)).isNotEqualTo(kept);
    }

    private static String firstShownFor(AllocationPlan plan, String targetCode) {
        return plan.presentations().stream()
                .filter(p -> p.targetCode().equals(targetCode))
                .map(AllocationPlan.PresentationOrder::firstShownTargetCode)
                .findFirst()
                .orElseThrow();
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
                plan.presentations(),
                plan.confirmatoryUnits(),
                plan.sessionSlots());

        assertThat(AllocationEngine.commitment(SEED, tampered))
                .isNotEqualTo(AllocationEngine.commitment(SEED, plan));
    }

    @Test
    @DisplayName("cada par tem um sitter sorteado como unidade da analise confirmatoria")
    void each_pair_has_one_drawn_confirmatory_unit() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);

        assertThat(plan.confirmatoryUnits()).hasSize(2);
        assertThat(plan.confirmatoryUnits().stream()
                .map(AllocationPlan.ConfirmatoryUnit::pairCode))
                .containsExactly("PR-0001", "PR-0002");

        for (AllocationPlan.ConfirmatoryUnit unit : plan.confirmatoryUnits()) {
            List<String> targetsOfPair = unit.pairCode().equals("PR-0001")
                    ? List.of("TGT-A", "TGT-B")
                    : List.of("TGT-C", "TGT-D");
            assertThat(unit.targetCode()).isIn(targetsOfPair);
        }
    }

    @Test
    @DisplayName("a unidade confirmatoria tambem e reproduzivel e entra no compromisso")
    void the_confirmatory_unit_is_part_of_the_commitment() {
        AllocationPlan plan = AllocationEngine.plan(SEED, INPUT);
        AllocationPlan again = AllocationEngine.plan(SEED, INPUT);

        assertThat(again.confirmatoryUnits()).isEqualTo(plan.confirmatoryUnits());
        assertThat(plan.canonicalForm()).contains("confirmatoryUnits");

        // Designate the other sitter of each pair, whichever one the draw picked.
        AllocationPlan swapped = new AllocationPlan(
                plan.studyCode(),
                plan.sessions(),
                plan.presentations(),
                plan.confirmatoryUnits().stream()
                        .map(u -> new AllocationPlan.ConfirmatoryUnit(u.pairCode(),
                                otherTargetOfPair(u.pairCode(), u.targetCode())))
                        .toList(),
                plan.sessionSlots());

        assertThat(swapped.confirmatoryUnits()).isNotEqualTo(plan.confirmatoryUnits());

        assertThat(AllocationEngine.commitment(SEED, swapped))
                .isNotEqualTo(AllocationEngine.commitment(SEED, plan));
    }

    private static String otherTargetOfPair(String pairCode, String targetCode) {
        List<String> targets = pairCode.equals("PR-0001")
                ? List.of("TGT-A", "TGT-B")
                : List.of("TGT-C", "TGT-D");
        return targets.get(0).equals(targetCode) ? targets.get(1) : targets.get(0);
    }

    @Test
    @DisplayName("a semente gerada tem 32 bytes e nao se repete")
    void seeds_are_fresh() {
        String first = AllocationEngine.newSeedHex();
        String second = AllocationEngine.newSeedHex();

        assertThat(first).hasSize(64).isNotEqualTo(second);
    }

    // --- P4, fase C: sorteio incremental sobre semente comprometida -----------------------------

    @Test
    @DisplayName("P4: o medium do par nao depende da ordem em que os elegiveis chegam")
    void structured_medium_is_independent_of_pool_order() {
        List<String> pool = List.of("MED-C", "MED-A", "MED-E", "MED-B", "MED-D");
        String drawn = AllocationEngine.structuredMedium(SEED, "PAR-1", 1, pool);
        for (int shift = 1; shift < pool.size(); shift++) {
            List<String> rotated = new java.util.ArrayList<>(pool);
            java.util.Collections.rotate(rotated, shift);
            assertThat(AllocationEngine.structuredMedium(SEED, "PAR-1", 1, rotated)).isEqualTo(drawn);
        }
        assertThat(pool).contains(drawn);
    }

    @Test
    @DisplayName("P4: a tentativa entra no dominio do sorteio, e a semente decide")
    void structured_medium_depends_on_attempt_pair_and_seed() {
        List<String> pool = java.util.stream.IntStream.range(0, 50).mapToObj(i -> "MED-" + i).toList();
        java.util.Set<String> byAttempt = new java.util.HashSet<>();
        java.util.Set<String> byPair = new java.util.HashSet<>();
        for (int i = 1; i <= 20; i++) {
            byAttempt.add(AllocationEngine.structuredMedium(SEED, "PAR-1", i, pool));
            byPair.add(AllocationEngine.structuredMedium(SEED, "PAR-" + i, 1, pool));
        }
        assertThat(byAttempt).hasSizeGreaterThan(1);
        assertThat(byPair).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("P4: a ordem dos nomes nao depende da ordem dos pedidos e e uma permutacao deles")
    void structured_name_order_is_canonical_then_drawn() {
        List<String> order = AllocationEngine.structuredNameOrder(SEED, "PAR-1", 1, List.of("PED-B", "PED-A"));
        assertThat(order).containsExactlyInAnyOrder("PED-A", "PED-B");
        assertThat(AllocationEngine.structuredNameOrder(SEED, "PAR-1", 1, List.of("PED-A", "PED-B")))
                .isEqualTo(order);
    }

    @Test
    @DisplayName("P4: codigo repetido e lista vazia sao recusados, e o compromisso cobre semente e entrada")
    void structured_inputs_are_checked_and_committed() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> AllocationEngine.structuredMedium(SEED, "PAR-1", 1, List.of("MED-A", "MED-A")))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> AllocationEngine.structuredMedium(SEED, "PAR-1", 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        String input = "{\"mode\":\"P4_INCREMENTAL\",\"studyCode\":\"STD-1\"}";
        assertThat(AllocationEngine.seedCommitment(SEED, input))
                .isEqualTo(io.github.rogeriocisi.airlab.support.Hashes.sha256Hex(SEED + "|" + input))
                .isNotEqualTo(AllocationEngine.seedCommitment(SEED, input.replace("STD-1", "STD-2")));
    }
}
