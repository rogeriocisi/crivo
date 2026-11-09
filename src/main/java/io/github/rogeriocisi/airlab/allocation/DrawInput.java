package io.github.rogeriocisi.airlab.allocation;

import java.util.List;

/**
 * Everything the draw depends on, in pseudonymous codes only.
 *
 * <p>This record is also the published input of the commitment: the draw is reproducible
 * only if an auditor can see exactly which pairs, mediums and experimenters were in play
 * (P5, P7). Order does not matter, because the engine sorts every list before drawing.
 *
 * @param studyCode        code of the study the draw belongs to
 * @param pairs            pairs of targets formed by the preregistered criteria
 * @param mediumCodes      mediums available for this study
 * @param experimenterCodes experimenters available for this study
 * @param sessionSlotsPerDay how many session slots a day holds, which is what the schedule is
 *                           counterbalanced over
 */
public record DrawInput(String studyCode,
                        List<PairInput> pairs,
                        List<String> mediumCodes,
                        List<String> experimenterCodes,
                        int sessionSlotsPerDay) {

    public DrawInput {
        if (pairs == null || pairs.isEmpty()) {
            throw new IllegalArgumentException("a draw needs at least one pair");
        }
        if (mediumCodes == null || mediumCodes.isEmpty()) {
            throw new IllegalArgumentException("a draw needs at least one medium");
        }
        if (experimenterCodes == null || experimenterCodes.isEmpty()) {
            throw new IllegalArgumentException("a draw needs at least one experimenter");
        }
        if (sessionSlotsPerDay < 1) {
            throw new IllegalArgumentException("a day needs at least one session slot");
        }
        pairs = List.copyOf(pairs);
        mediumCodes = List.copyOf(mediumCodes);
        experimenterCodes = List.copyOf(experimenterCodes);
    }

    /** A pair of targets with similar profile: each one is the decoy of the other. */
    public record PairInput(String pairCode, String targetA, String targetB) {
        public PairInput {
            if (targetA.equals(targetB)) {
                throw new IllegalArgumentException("a pair needs two distinct targets");
            }
        }
    }

    /**
     * Canonical JSON of the input, stored with the draw and republished in the verification
     * package. The keys match this record, so the stored form reads back into it without a
     * mapping layer in between.
     */
    public String canonicalForm() {
        java.util.List<Object> pairList = new java.util.ArrayList<>();
        for (PairInput pair : pairs) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("pairCode", pair.pairCode());
            row.put("targetA", pair.targetA());
            row.put("targetB", pair.targetB());
            pairList.add(row);
        }
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("studyCode", studyCode);
        root.put("pairs", pairList);
        root.put("mediumCodes", mediumCodes);
        root.put("experimenterCodes", experimenterCodes);
        root.put("sessionSlotsPerDay", sessionSlotsPerDay);
        return io.github.rogeriocisi.airlab.support.CanonicalJson.write(root);
    }
}
