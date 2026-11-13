package io.github.rogeriocisi.airlab.structured;

/**
 * What one run of the P4 cycle did, in counts. Logged and returned to tests; nothing in it
 * links a pair to a medium.
 */
public record CycleReport(int dissolvedPairs,
                          int expiredAssignments,
                          int cancelledAssignments,
                          int formedPairs,
                          int sentPairs,
                          int unblindedStudies) {

    public static CycleReport none() {
        return new CycleReport(0, 0, 0, 0, 0, 0);
    }

    public boolean idle() {
        return dissolvedPairs + expiredAssignments + cancelledAssignments + formedPairs + sentPairs
                + unblindedStudies == 0;
    }
}
