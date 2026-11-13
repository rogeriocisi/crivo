package io.github.rogeriocisi.airlab.structured;

/**
 * Where the P4 study stands, for the administrator: counts of units, never performance (E10,
 * lock 1). No code, no medium, nothing that links a pair to whoever reads it.
 *
 * @param targetPairs          N declared in the preregistration (0 when there is no study)
 * @param waitingRequests      locked requests in the frozen questionnaire version, without a pair
 * @param activePairs          pairs formed and not dissolved
 * @param pairsWaitingMedium   active pairs with no open or completed request to a medium — no
 *                             eligible medium yet, or every eligible one at the cap
 * @param openAssignments      requests to a medium awaiting the readings
 * @param completedPairs       active pairs whose medium locked both readings
 * @param lockedReadings       readings locked, in any request to a medium
 * @param expiredAssignments   requests to a medium that ran past the deadline
 * @param cancelledAssignments requests to a medium cancelled by a closed account or a dissolved pair
 * @param dissolvedPairs       pairs undone by a withdrawal
 */
public record StructuredProgress(int targetPairs,
                                 long waitingRequests,
                                 long activePairs,
                                 long pairsWaitingMedium,
                                 long openAssignments,
                                 long completedPairs,
                                 long lockedReadings,
                                 long expiredAssignments,
                                 long cancelledAssignments,
                                 long dissolvedPairs) {

    public static StructuredProgress none() {
        return new StructuredProgress(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
