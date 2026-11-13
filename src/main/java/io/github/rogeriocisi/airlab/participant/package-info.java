/**
 * People, consent, targets and pairing (Epic E2, Constitution P6).
 *
 * <p>The only module allowed to read or write the {@code identity} schema (architectural
 * rule 2). An architecture test asserts that no other module references its entities.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "participant",
        allowedDependencies = {"audit", "catalog", "support"})
package io.github.rogeriocisi.airlab.participant;
