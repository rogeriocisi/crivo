package io.github.rogeriocisi.airlab.participant;

/**
 * Domain event: a P4 consultation request was locked and its commitment published. Carries the
 * pseudonymous request code only.
 */
public record ConsultationLocked(String code) {
}
