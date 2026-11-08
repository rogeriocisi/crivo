package io.github.rogeriocisi.airlab.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The paired primary measure: for each pair, the designated sitter's score for the target
 * reading minus the score for the decoy reading (Epic E7).
 *
 * <p>One observation per pair, so the pairs stay independent, while the measurement keeps
 * its full range instead of collapsing to a single bit. That is the whole point: the forced
 * choice throws away the size of the difference, and with the effect the literature reports
 * that loss is the difference between needing about 30 pairs and needing a few hundred.
 *
 * <p>Strict for the same reason as {@link IndependentUnits}: a missing or duplicated pair
 * makes it throw rather than quietly change the denominator.
 */
public final class PairedComparison {

    private PairedComparison() {
    }

    /**
     * One pair's contribution.
     *
     * @param targetScore score the designated sitter gave the reading of their own target
     * @param decoyScore  score the same sitter gave the decoy reading
     */
    public record PairScore(String pairCode, double targetScore, double decoyScore) {

        public double difference() {
            return targetScore - decoyScore;
        }
    }

    /** Tests whether target scores exceed decoy scores, one-sided, as preregistered. */
    public static WilcoxonResult targetExceedsDecoy(List<PairScore> pairScores) {
        if (pairScores.isEmpty()) {
            throw new IllegalArgumentException("the paired analysis needs at least one pair");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<PairScore> ordered = new ArrayList<>(pairScores);
        ordered.sort(Comparator.comparing(PairScore::pairCode));

        List<Double> differences = new ArrayList<>();
        for (PairScore score : ordered) {
            if (!seen.add(score.pairCode())) {
                throw new IllegalArgumentException(
                        "more than one score for pair " + score.pairCode());
            }
            differences.add(score.difference());
        }
        return WilcoxonSignedRank.greaterThanZero(differences);
    }
}
