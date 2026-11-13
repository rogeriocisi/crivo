package io.github.rogeriocisi.airlab.structured.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The P4 pairing rule (SPEC E13, phase C) as a pure function: same stratum, by lock time, two by
 * two, never two requests of the same sitter, and the same answer whatever order the rows come in.
 */
class StructuredPairingTest {

    private static ConsultationRef request(String code, String sitter, String gender, String age, String period,
                                           long second) {
        return new ConsultationRef(code, sitter, "v1", gender, age, period, ConsultationState.LOCKED,
                Instant.EPOCH.plusSeconds(second));
    }

    private static List<List<String>> codes(List<StructuredPairing.Pairing> pairings) {
        return pairings.stream().map(p -> List.of(p.first().code(), p.second().code())).toList();
    }

    @Test
    @DisplayName("pareia dentro do estrato, por ordem de travamento, e nunca entre estratos")
    void pairs_within_the_stratum_by_lock_time() {
        List<ConsultationRef> waiting = List.of(
                request("PED-1", "CON-1", "feminino", "60-79", "1-5-anos", 1),
                request("PED-2", "CON-2", "masculino", "60-79", "1-5-anos", 2),
                request("PED-3", "CON-3", "feminino", "60-79", "1-5-anos", 3),
                request("PED-4", "CON-4", "feminino", "60-79", "1-5-anos", 4),
                request("PED-5", "CON-5", "feminino", "40-59", "1-5-anos", 5));

        assertThat(codes(StructuredPairing.form(waiting, 10))).containsExactly(List.of("PED-1", "PED-3"));
    }

    @Test
    @DisplayName("o resultado nao depende da ordem de entrada")
    void result_does_not_depend_on_input_order() {
        List<ConsultationRef> waiting = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            waiting.add(request("PED-" + i, "CON-" + i, i % 2 == 0 ? "feminino" : "masculino",
                    i % 3 == 0 ? "60-79" : "40-59", "1-5-anos", i));
        }
        List<List<String>> expected = codes(StructuredPairing.form(waiting, 100));
        for (int seed = 0; seed < 5; seed++) {
            List<ConsultationRef> shuffled = new ArrayList<>(waiting);
            Collections.shuffle(shuffled, new java.util.Random(seed));
            assertThat(codes(StructuredPairing.form(shuffled, 100))).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("o limite de N pares fica com os pares que se tornaram possiveis primeiro")
    void the_limit_keeps_the_earliest_possible_pairs() {
        List<ConsultationRef> waiting = List.of(
                request("PED-A1", "CON-1", "feminino", "60-79", "1-5-anos", 1),
                request("PED-B1", "CON-2", "masculino", "18-39", "mais-15-anos", 2),
                request("PED-B2", "CON-3", "masculino", "18-39", "mais-15-anos", 3),
                request("PED-A2", "CON-4", "feminino", "60-79", "1-5-anos", 9));

        assertThat(codes(StructuredPairing.form(waiting, 1))).containsExactly(List.of("PED-B1", "PED-B2"));
        assertThat(StructuredPairing.form(waiting, 0)).isEmpty();
    }

    @Test
    @DisplayName("dois pedidos do mesmo consulente nunca formam par")
    void never_two_requests_of_the_same_sitter() {
        List<ConsultationRef> waiting = List.of(
                request("PED-1", "CON-1", "feminino", "60-79", "1-5-anos", 1),
                request("PED-2", "CON-1", "feminino", "60-79", "1-5-anos", 2),
                request("PED-3", "CON-3", "feminino", "60-79", "1-5-anos", 3));

        assertThat(codes(StructuredPairing.form(waiting, 10))).containsExactly(List.of("PED-1", "PED-3"));
    }

    @Test
    @DisplayName("dois consulentes sinalizados entre si nunca formam par")
    void flagged_sitters_are_never_paired() {
        List<ConsultationRef> waiting = List.of(
                request("PED-1", "CON-1", "feminino", "60-79", "1-5-anos", 1),
                request("PED-2", "CON-2", "feminino", "60-79", "1-5-anos", 2),
                request("PED-3", "CON-3", "feminino", "60-79", "1-5-anos", 3));

        List<StructuredPairing.Pairing> pairings = StructuredPairing.form(waiting, 10,
                (one, other) -> java.util.Set.of(one, other).equals(java.util.Set.of("CON-1", "CON-2")));

        assertThat(codes(pairings)).containsExactly(List.of("PED-1", "PED-3"));
    }
}
