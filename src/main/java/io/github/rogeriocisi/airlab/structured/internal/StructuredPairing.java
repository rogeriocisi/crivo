package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The P4 pairing rule (SPEC E13, phase C), as a pure function so a test — and, later, the
 * verification package — can rerun it.
 *
 * <p>Same stratum (gender, age band, period of death); within a stratum, by lock time and then
 * code, two by two; requests of the same sitter never together. The input is sorted here, so the
 * order in which the database returned the rows cannot change who is paired with whom (P5).
 * When the limit binds, the pairs that became possible first — the earlier of the two second
 * locks — go first.
 */
final class StructuredPairing {

    private StructuredPairing() {
    }

    record Pairing(ConsultationRef first, ConsultationRef second) {

        /** When the pair became possible: the later of the two locks. */
        java.time.Instant possibleSince() {
            return first.lockedAt().isAfter(second.lockedAt()) ? first.lockedAt() : second.lockedAt();
        }
    }

    static List<Pairing> form(List<ConsultationRef> waiting, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ConsultationRef> sorted = new ArrayList<>(waiting);
        sorted.sort(Comparator.comparing(ConsultationRef::lockedAt).thenComparing(ConsultationRef::code));

        Map<String, List<ConsultationRef>> strata = new LinkedHashMap<>();
        for (ConsultationRef request : sorted) {
            strata.computeIfAbsent(request.stratum(), key -> new ArrayList<>()).add(request);
        }

        List<Pairing> pairings = new ArrayList<>();
        for (List<ConsultationRef> stratum : strata.values()) {
            List<ConsultationRef> remaining = new ArrayList<>(stratum);
            while (remaining.size() >= 2) {
                ConsultationRef first = remaining.remove(0);
                int match = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (!remaining.get(i).sitterCode().equals(first.sitterCode())) {
                        match = i;
                        break;
                    }
                }
                if (match < 0) {
                    continue;
                }
                pairings.add(new Pairing(first, remaining.remove(match)));
            }
        }

        pairings.sort(Comparator.comparing(Pairing::possibleSince)
                .thenComparing(pairing -> pairing.first().code()));
        return pairings.size() > limit ? List.copyOf(pairings.subList(0, limit)) : List.copyOf(pairings);
    }
}
