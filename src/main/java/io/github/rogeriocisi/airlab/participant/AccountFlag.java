package io.github.rogeriocisi.airlab.participant;

import java.time.Instant;

/**
 * Two accounts that share a signal, for human review (SPEC E13, phase E). Pseudonymous codes and
 * profiles only: the administrator decides from the kind of signal, without names or addresses.
 *
 * @param decision null while pending
 */
public record AccountFlag(String code,
                          String accountA,
                          AccountProfile profileA,
                          String accountB,
                          AccountProfile profileB,
                          LinkSignal signal,
                          Instant createdAt,
                          FlagDecision decision,
                          Instant reviewedAt) {

    public boolean pending() {
        return decision == null;
    }

    /** Whether the two accounts must be kept apart in the draw: pending, or confirmed linked. */
    public boolean keepsApart() {
        return decision != FlagDecision.UNRELATED;
    }
}
