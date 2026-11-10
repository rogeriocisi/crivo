package io.github.rogeriocisi.airlab.trials;

/**
 * Life cycle of a self-service session.
 *
 * <p>An abandoned session is recorded as abandoned and stays in the data. It counts in the
 * denominator of any pool analysis, by the preregistered rule: giving up on a session that
 * feels bad must not improve the aggregate (E11).
 */
public enum SessionState {

    /** Running. Trials may still be submitted. */
    ACTIVE,

    /** Every declared trial was locked, and the seed was revealed. */
    FINISHED,

    /** Left incomplete. The seed is revealed too, so the session stays auditable. */
    ABANDONED
}
