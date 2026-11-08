package io.github.rogeriocisi.airlab.study;

import java.time.Instant;

/**
 * Study projection for the researcher. Carries no participant data and no reading link, so
 * it is safe to render on any researcher screen (SPEC section 4).
 */
public record StudySummary(long id, String code, String title, String protocol,
                           StudyStatus status, Instant createdAt) {
}
