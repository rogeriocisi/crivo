package io.github.rogeriocisi.airlab.trials;

import java.util.List;
import java.util.Optional;

/**
 * One trial, after the session is over.
 *
 * @param chosenIndex        presentation position the person picked, or the one they ranked
 *                           first in remote viewing
 * @param targetIndex        presentation position of the target, recomputable from the seed
 * @param candidateSlugs     figures shown, in presentation order; empty for the forced choice
 * @param impressionSha256   hash of the impression as it was locked, before the candidates
 *                           appeared, so it can be shown that it was not rewritten afterwards
 */
public record TrialOutcome(int index,
                           int chosenIndex,
                           int targetIndex,
                           boolean hit,
                           List<String> candidateSlugs,
                           Optional<String> impression,
                           Optional<String> impressionSha256) {

    public TrialOutcome {
        candidateSlugs = List.copyOf(candidateSlugs);
    }
}
