package io.github.rogeriocisi.airlab.participant;

import java.time.Instant;

/**
 * An account as its owner sees it. Built only for the owner: the visibility matrix of SPEC E13
 * says each person sees only their own identity.
 */
public record AccountView(String code,
                          AccountProfile profile,
                          String firstName,
                          String termVersion,
                          Instant createdAt,
                          Instant emailConfirmedAt) {
}
