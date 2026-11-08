package io.github.rogeriocisi.airlab.study;

import java.time.Instant;
import java.util.Optional;

/**
 * One preregistration version as published.
 *
 * @param document the canonical document, exactly the bytes {@code sha256} was computed over
 * @param sha256   the commitment published before the first session (P2)
 */
public record PreregistrationSnapshot(long studyId,
                                      int version,
                                      String document,
                                      String sha256,
                                      Optional<Instant> frozenAt,
                                      Optional<String> timestampProofRef,
                                      Optional<String> amendmentReason,
                                      Instant createdAt) {

    public boolean frozen() {
        return frozenAt.isPresent();
    }
}
