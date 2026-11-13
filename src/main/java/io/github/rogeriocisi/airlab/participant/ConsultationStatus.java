package io.github.rogeriocisi.airlab.participant;

import java.time.Instant;

/**
 * A P4 request as its panel shows it: where it stands and the public commitment. No first name,
 * no answers, no score — the panel needs none of them to say where the request is.
 */
public record ConsultationStatus(String code,
                                 ConsultationState state,
                                 String questionnaireVersion,
                                 String commitment,
                                 Instant lockedAt) {
}
