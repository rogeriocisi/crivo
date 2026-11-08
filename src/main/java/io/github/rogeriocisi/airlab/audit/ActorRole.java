package io.github.rogeriocisi.airlab.audit;

/**
 * Who performed an audited operation. The actor is recorded by pseudonymous code only (P6).
 */
public enum ActorRole {
    RESEARCHER,
    EXPERIMENTER,
    MEDIUM,
    REDACTOR,
    SITTER,
    AUDITOR,
    SYSTEM
}
