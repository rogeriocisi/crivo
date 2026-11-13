package io.github.rogeriocisi.airlab.structured;

import java.time.Instant;

/**
 * Domain event: a pair was sent to the medium drawn for it (SPEC E13, phase C). In-process only:
 * the notification module uses the medium's code to send that medium an e-mail without names,
 * and the administrator's notice to it carries the pair code alone. Never written to the trail,
 * which would link the pair to the medium before the unblinding.
 */
public record ReadingRequestSent(String pairCode, String assignmentCode, String mediumCode, Instant dueAt) {
}
