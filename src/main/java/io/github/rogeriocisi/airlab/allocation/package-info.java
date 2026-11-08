/**
 * Verifiable draw with commitment and later reveal (Epic E3, Constitution P5).
 *
 * <p>{@code AllocationEngine} is a pure function on purpose: the whole point of P5 is that
 * a third party can rerun it. Anything that needs a clock, a database or a user goes in
 * {@code internal}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "allocation",
        allowedDependencies = {"audit", "support"})
package io.github.rogeriocisi.airlab.allocation;
