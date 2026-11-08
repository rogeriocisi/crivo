package io.github.rogeriocisi.airlab.allocation;

import java.time.Instant;
import java.util.Optional;

/**
 * What an auditor needs to recheck a draw: the commitment, the exact bytes it was computed
 * over, and the seed once revealed.
 *
 * @param seedHex empty while the study is still blind
 */
public record AllocationSnapshot(long studyId,
                                 String commitment,
                                 String canonicalDrawInput,
                                 String canonicalAllocations,
                                 Instant drawnAt,
                                 Optional<String> seedHex,
                                 Optional<Instant> seedRevealedAt) {
}
