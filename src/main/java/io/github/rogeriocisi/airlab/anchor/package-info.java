/**
 * External anchoring of the audit trail by RFC 3161 time stamp (SPEC E8, v0.15).
 *
 * <p>The head of the trail is stamped at an independent time stamp authority (FreeTSA, phase 1):
 * right after each milestone — preregistration frozen or amended, seed revealed, report generated —
 * and once a day when the trail changed. Only the 32 byte hash leaves the server, never content
 * (P6). The token is checked before it is stored, stored append-only, published for anyone to check
 * with {@code openssl ts -verify}, and checked again by {@code airlab verify}.
 *
 * <p>Depends on the audit trail and nothing else from the domain: anchoring must keep working for
 * any protocol, and a protocol must not be able to skip it.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "anchor",
        allowedDependencies = {"audit", "support"})
package io.github.rogeriocisi.airlab.anchor;
