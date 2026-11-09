package io.github.rogeriocisi.airlab.study;

import io.github.rogeriocisi.airlab.analysis.AnalysisUnit;
import io.github.rogeriocisi.airlab.analysis.PrimaryMeasure;
import io.github.rogeriocisi.airlab.analysis.StoppingRule;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The preregistration content: what the study claims, who takes part, how it will be scored,
 * when collection stops and how it will be analysed, all written before any data exists
 * (Constitution P2, Epic E1).
 *
 * <p>Every field is part of the hashed document. Changing any of them after the freeze is
 * impossible: it takes an amendment, which creates a new version and keeps the old one
 * visible.
 *
 * @param hypotheses              the hypotheses, in the researcher's own words
 * @param mediumInclusionCriteria how mediums are recruited and screened, including any
 *                                selection before the study. Declared in advance because
 *                                selecting mediums on past performance inflates the effect,
 *                                and the literature's positive results come from
 *                                "selected mediums" without the criteria being published
 * @param pairingCriteria         how two targets are judged to be of similar profile
 * @param scoringScale            the item scale, exactly as the sitter will see it
 * @param primaryAnalysis         the single analysis that counts as confirmatory
 * @param primaryMeasure          the quantity that analysis tests
 * @param expectedEffect          the effect the sample size was computed for: the expected
 *                                hit rate for a forced choice, the expected standardised
 *                                difference d for a paired measure. Declared in advance so
 *                                that {@link PowerCheck} can state, in the frozen document,
 *                                what the design can and cannot detect
 * @param analysisUnit            what counts as one independent observation
 * @param stoppingRule            when collection stops
 * @param targetSampleSize        planned number of independent units, in the unit above
 * @param maxItemCountDifference  how many items the two readings of a pair may differ by
 *                                before the longer one is cut to length by a seed-drawn
 *                                subsample. Declared in advance because the length of a
 *                                reading drives the ratings on its own: in O'Keeffe and
 *                                Wiseman's data the readings ran from 6 to 92 statements and
 *                                the longest, most general one was rated highest by every
 *                                sitter
 * @param secondaryAnalyses       analyses also declared in advance
 * @param exclusionRules          when a session or a packet is excluded, decided in advance
 */
public record PreregistrationPlan(@NotEmpty List<@NotBlank String> hypotheses,
                                  @NotBlank String mediumInclusionCriteria,
                                  @NotBlank String pairingCriteria,
                                  @NotEmpty List<@NotBlank String> scoringScale,
                                  @NotBlank String primaryAnalysis,
                                  @NotNull PrimaryMeasure primaryMeasure,
                                  @Positive double expectedEffect,
                                  @NotNull AnalysisUnit analysisUnit,
                                  @NotNull StoppingRule stoppingRule,
                                  @Min(1) int targetSampleSize,
                                  @Min(0) int maxItemCountDifference,
                                  List<String> secondaryAnalyses,
                                  List<String> exclusionRules) {

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
        root.put("mediumInclusionCriteria", mediumInclusionCriteria);
        root.put("pairingCriteria", pairingCriteria);
        root.put("scoringScale", scoringScale);
        root.put("primaryAnalysis", primaryAnalysis);
        root.put("primaryMeasure", primaryMeasure.name());
        root.put("expectedEffect", expectedEffect);
        root.put("analysisUnit", analysisUnit.name());
        root.put("stoppingRule", stoppingRule.name());
        root.put("targetSampleSize", targetSampleSize);
        root.put("maxItemCountDifference", maxItemCountDifference);
        root.put("secondaryAnalyses", secondaryAnalyses);
        root.put("exclusionRules", exclusionRules);
        return CanonicalJson.write(root);
    }
}
