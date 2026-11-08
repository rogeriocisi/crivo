package io.github.rogeriocisi.airlab.study;

/**
 * Life cycle of a study. The order is one way: a study never goes back to a previous state,
 * because every transition is already recorded in the audit trail (P4).
 */
public enum StudyStatus {

    /** Preregistration still being written. No participant data, no sessions. */
    DRAFT,

    /** Preregistration frozen and committed, draw done. Sessions may start (P2). */
    PREREGISTERED,

    /** Sessions, redaction and blind scoring under way. */
    IN_FIELD,

    /** Every scoring packet locked. The unblinding becomes possible. */
    SCORING_CLOSED,

    /** Seed revealed, reading-to-target link available. Irreversible. */
    UNBLINDED,

    /** Analysis run and report generated. */
    REPORTED
}
