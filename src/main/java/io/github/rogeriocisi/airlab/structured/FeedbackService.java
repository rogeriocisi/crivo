package io.github.rogeriocisi.airlab.structured;

import java.util.List;
import java.util.Optional;

/**
 * The feedback of the P4 (SPEC E13, phase D), after the unblinding and only then: it is built from
 * the scores, and the scores do not exist before (the database refuses them).
 */
public interface FeedbackService {

    /**
     * The feedback for one consultation request. The caller passes the sitter's own request code,
     * taken from the signed-in sitter; empty until the pair is scored.
     */
    Optional<TargetFeedback> forRequest(String requestCode);

    /** Counts for each of the medium's readings in scored pairs, newest first. */
    List<ReadingFeedback> forMedium(String mediumCode);
}
