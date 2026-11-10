package io.github.rogeriocisi.airlab.trials;

import java.util.List;
import java.util.Optional;

/**
 * The self-service tests of the catalog (Epics E11 and E12).
 *
 * <p>Note what this API does not offer: no way to read the target of an open trial, no way to
 * read the seed before the session closes, and no running score. Those are not omissions to be
 * filled in later — they are the protocol.
 */
public interface TrialSessionService {

    /**
     * Opens a session: draws the seed, publishes its commitment and fixes the number of
     * trials. The number cannot change afterwards (E11).
     */
    SessionProgress start(TrialProtocol protocol, int declaredTrials);

    SessionProgress progress(String sessionCode);

    /**
     * Records the participant's half of the seed, drawn in their own browser after seeing the
     * commitment to the server's half (Epic E11).
     *
     * <p>Until this happens the session has no seed at all, and therefore no targets: the
     * database refuses to hold a trial for a session without it. That ordering is the whole
     * guarantee — the server was already committed when the nonce arrived, and the nonce was
     * unpredictable to it, so neither side chose the outcome.
     *
     * @param nonceHex 16 to 32 bytes in hexadecimal
     */
    SessionProgress contributeEntropy(String sessionCode, String nonceHex);

    /**
     * Locks the written impression of the current remote viewing trial and only then returns
     * the four figures. Calling it twice for the same trial is refused: the impression cannot
     * be rewritten after the candidates are seen (E12).
     */
    TrialCandidates lockImpression(String sessionCode, String impression);

    /** The candidates of the current trial, once its impression is locked. */
    TrialCandidates currentCandidates(String sessionCode);

    /**
     * Records the answer of the current trial and locks it. The target is computed at this
     * moment, never before.
     *
     * @param chosenPosition presentation position chosen, from 1
     */
    SessionProgress submitChoice(String sessionCode, int chosenPosition);

    /**
     * Records the ranking of a remote viewing trial, most similar first, and locks it.
     *
     * @param rankedPositions presentation positions in order of similarity, a permutation of
     *                        1..alternatives
     */
    SessionProgress submitRanking(String sessionCode, List<Integer> rankedPositions);

    /**
     * Closes the session, reveals the seed and computes the outcome. Idempotent: calling it
     * again returns the same outcome rather than recomputing or redrawing anything.
     */
    SessionOutcome finish(String sessionCode);

    /** Marks an incomplete session as abandoned, revealing the seed as well. */
    SessionOutcome abandon(String sessionCode);

    /** The outcome, if and only if the session is closed. */
    Optional<SessionOutcome> outcome(String sessionCode);
}
