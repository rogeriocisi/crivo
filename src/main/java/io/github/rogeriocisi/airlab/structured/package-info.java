/**
 * The automated structured reading, P4 (SPEC E13, phases C and D): the P4 study, the pairing of
 * locked requests, the medium draw, the requests to the medium with their deadline, and the
 * medium's locked readings.
 *
 * <p>No identifiable data lives here. The first names stay in {@code participant}, which hands
 * them only to a medium holding a recorded release; this module keeps codes, states, the
 * eligible pools and the readings, encrypted. The link between a pair and its medium is in the
 * database and nowhere else — not in the public trail, not on an administrator screen — until
 * the unblinding (P4 visibility matrix).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "structured",
        allowedDependencies = {"allocation", "analysis", "audit", "catalog", "participant", "study", "support"})
package io.github.rogeriocisi.airlab.structured;
