package io.github.rogeriocisi.airlab.trials;

import java.time.Instant;

/**
 * What a person may see while their session is still running (E10, lock 1; E11).
 *
 * <p>Read the fields: counts and a commitment hash, and nothing else. There is no hit, no
 * score, no target and no seed here, and that is by construction rather than by filtering in
 * a template — {@code BlindingMatrixTest} fails if a field with such a name appears.
 *
 * <p>Without this, a self-service test becomes an optional stopping machine: a person who sees
 * a running tally stops at a good moment, and so does a researcher watching a public pool.
 *
 * @param seedCommitment commitment to the server's half of the seed, published before the
 *                       participant contributes theirs
 * @param awaitingEntropy true while the session still needs the participant's half of the seed;
 *                        no trial exists before that
 */
public record SessionProgress(String sessionCode,
                              TrialProtocol protocol,
                              int declaredTrials,
                              int lockedTrials,
                              int nextTrialIndex,
                              String seedCommitment,
                              Instant startedAt,
                              SessionState state,
                              boolean awaitingEntropy,
                              boolean awaitingImpression) {

    public int remainingTrials() {
        return Math.max(0, declaredTrials - lockedTrials);
    }

    public boolean finished() {
        return state == SessionState.FINISHED;
    }
}
