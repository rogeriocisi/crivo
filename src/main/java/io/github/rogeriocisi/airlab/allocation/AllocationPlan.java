package io.github.rogeriocisi.airlab.allocation;

import io.github.rogeriocisi.airlab.support.CanonicalJson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of a draw: which medium reads which target, with which experimenter, in which
 * order, and in which order the two readings of a pair are shown to each sitter.
 *
 * <p>The plan is the object the commitment is computed over, so its canonical form is part
 * of the protocol and must not change shape without a new preregistration version (P2, P5).
 */
public record AllocationPlan(String studyCode,
                             List<SessionAllocation> sessions,
                             List<PresentationOrder> presentations) {

    public AllocationPlan {
        sessions = List.copyOf(sessions);
        presentations = List.copyOf(presentations);
    }

    /**
     * One reading session.
     *
     * @param readingOrder 1 or 2: the order in which the medium reads the two targets of
     *                     the pair, drawn so that being read first is not a clue
     */
    public record SessionAllocation(String pairCode,
                                    String targetCode,
                                    String mediumCode,
                                    String experimenterCode,
                                    int readingOrder) {
    }

    /**
     * For the sitter of {@code targetCode}, which target's redacted reading is shown first.
     * The sitter never learns that this mapping exists (P1).
     */
    public record PresentationOrder(String targetCode, String firstShownTargetCode) {
    }

    /** Canonical JSON of the plan: the exact bytes the commitment is computed over. */
    public String canonicalForm() {
        List<Object> sessionList = new ArrayList<>();
        for (SessionAllocation s : sessions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pair", s.pairCode());
            row.put("target", s.targetCode());
            row.put("medium", s.mediumCode());
            row.put("experimenter", s.experimenterCode());
            row.put("readingOrder", s.readingOrder());
            sessionList.add(row);
        }
        List<Object> presentationList = new ArrayList<>();
        for (PresentationOrder p : presentations) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("target", p.targetCode());
            row.put("firstShown", p.firstShownTargetCode());
            presentationList.add(row);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("study", studyCode);
        root.put("sessions", sessionList);
        root.put("presentations", presentationList);
        return CanonicalJson.write(root);
    }
}
