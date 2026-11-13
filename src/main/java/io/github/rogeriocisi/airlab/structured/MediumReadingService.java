package io.github.rogeriocisi.airlab.structured;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The medium's side of the P4 (SPEC E13, phase C): the requests drawn for them, and the locking
 * of each reading. Every method takes the medium's own code, and nothing here answers for anyone
 * else's request.
 */
public interface MediumReadingService {

    /** The medium's requests that are open or completed, newest first. Expired ones drop out. */
    List<MediumAssignment> assignments(String mediumCode);

    /** One of the medium's own requests; empty for anyone else's, or for an expired one. */
    Optional<MediumAssignment> assignment(String mediumCode, String assignmentCode);

    /** Requests ever sent to this medium that expired or were cancelled, for the panel. */
    long closedWithoutReading(String mediumCode);

    /**
     * Checks a reading without storing anything, so the review page can show errors first.
     *
     * @throws IllegalArgumentException with a message in Portuguese, for the medium
     */
    void check(String questionnaireVersion, Map<String, String> answers);

    /**
     * Locks one reading: stores it encrypted, publishes its salted commitment in the trail without
     * the medium's code, and — when it is the second of the pair — closes the request to the
     * medium and marks both consultation requests as read.
     *
     * @throws IllegalArgumentException when the answers are invalid
     * @throws IllegalStateException    when the request is not this medium's, is no longer open,
     *                                  or that position is already locked
     */
    void lock(String mediumCode, String assignmentCode, int position, Map<String, String> answers);
}
