package io.github.rogeriocisi.airlab.participant;

/**
 * Roles a person can hold in a study. Experimenter and redactor must be different people
 * (SPEC section 4, note); when the pilot cannot manage that, the deviation is recorded as a
 * limitation in the preregistration rather than hidden.
 */
public enum ParticipantRole {
    RESEARCHER,
    EXPERIMENTER,
    MEDIUM,
    REDACTOR,
    SITTER
}
