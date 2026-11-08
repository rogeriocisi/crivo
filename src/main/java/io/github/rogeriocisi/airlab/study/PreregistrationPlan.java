package io.github.rogeriocisi.airlab.study;

import io.github.rogeriocisi.airlab.support.CanonicalJson;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The preregistration content: what the study claims, how it will be scored and how it will
 * be analysed, written before any data exists (Constitution P2, Epic E1).
 *
 * <p>Every field is part of the hashed document. Changing any of them after the freeze is
 * impossible: it takes an amendment, which creates a new version and keeps the old one
 * visible.
 *
 * @param hypotheses        the hypotheses, in the researcher's own words
 * @param pairingCriteria   how two targets are judged to be of similar profile
 * @param scoringScale      the item scale, exactly as the sitter will see it
 * @param primaryAnalysis   the single analysis that counts as confirmatory
 * @param secondaryAnalyses analyses also declared in advance
 * @param exclusionRules    when a session or a packet is excluded, decided in advance
 * @param targetSampleSize  planned number of independent forced choices
 */
public record PreregistrationPlan(@NotEmpty List<@NotBlank String> hypotheses,
                                  @NotBlank String pairingCriteria,
                                  @NotEmpty List<@NotBlank String> scoringScale,
                                  @NotBlank String primaryAnalysis,
                                  List<String> secondaryAnalyses,
                                  List<String> exclusionRules,
                                  @Min(1) int targetSampleSize) {

    public PreregistrationPlan {
        hypotheses = List.copyOf(hypotheses);
        scoringScale = List.copyOf(scoringScale);
        secondaryAnalyses = secondaryAnalyses == null ? List.of() : List.copyOf(secondaryAnalyses);
        exclusionRules = exclusionRules == null ? List.of() : List.copyOf(exclusionRules);
    }

    /** Canonical JSON of the plan, embedded in the document that gets hashed. */
    public String canonicalForm() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("hypotheses", hypotheses);
        root.put("pairingCriteria", pairingCriteria);
        root.put("scoringScale", scoringScale);
        root.put("primaryAnalysis", primaryAnalysis);
        root.put("secondaryAnalyses", secondaryAnalyses);
        root.put("exclusionRules", exclusionRules);
        root.put("targetSampleSize", targetSampleSize);
        return CanonicalJson.write(root);
    }
}
