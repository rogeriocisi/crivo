package io.github.rogeriocisi.airlab.participant;

/** What happened to a confirmation link. */
public enum EmailConfirmationResult {
    /** The account is now confirmed. */
    CONFIRMED,
    /** The link had already been used; the account was confirmed then. */
    ALREADY_USED,
    /** The link expired before use; signing up again sends a new one. */
    EXPIRED,
    /** No such link. */
    UNKNOWN
}
