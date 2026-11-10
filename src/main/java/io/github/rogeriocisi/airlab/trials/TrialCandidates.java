package io.github.rogeriocisi.airlab.trials;

import java.util.List;

/**
 * The four figures of a remote viewing trial, in presentation order, handed over only after
 * the impression is locked (E12).
 *
 * <p>Which of them is the target is not in this record, and no API returns it while the trial
 * is open. The person ranks the figures; the target position is computed when the ranking is
 * locked.
 */
public record TrialCandidates(String sessionCode,
                              int trialIndex,
                              String targetPoolVersion,
                              List<Candidate> candidates) {

    public TrialCandidates {
        candidates = List.copyOf(candidates);
    }

    /**
     * @param position presentation position, from 1, which is what the person ranks
     */
    public record Candidate(int position, String slug, String label, String imagePath) {
    }
}
