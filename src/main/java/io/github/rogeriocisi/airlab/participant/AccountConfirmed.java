package io.github.rogeriocisi.airlab.participant;

/**
 * Domain event: an account confirmed its e-mail and is now valid. Carries the pseudonymous code
 * and the profile, never personal data, so any listener can store or forward it.
 */
public record AccountConfirmed(String code, AccountProfile profile) {
}
