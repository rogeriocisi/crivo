package io.github.rogeriocisi.airlab.allocation;

/**
 * Published when a draw completes. The session module listens for it and creates the
 * sessions; nothing else may consume the plan.
 *
 * <p>The event carries the plan but never the seed: the seed stays inside this module until
 * the unblinding (P1, P5).
 */
public record AllocationDrawn(long studyId, String commitment, AllocationPlan plan) {
}
