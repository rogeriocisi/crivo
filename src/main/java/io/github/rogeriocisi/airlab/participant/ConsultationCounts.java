package io.github.rogeriocisi.airlab.participant;

/**
 * P4 requests by situation, for the administrator. Counts of units, never performance
 * (E10, lock 1).
 */
public record ConsultationCounts(long locked, long withdrawn) {
}
