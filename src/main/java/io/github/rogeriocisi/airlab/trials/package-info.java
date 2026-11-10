/**
 * The self-service tests of the catalog: precognitive forced choice (E11) and ranked remote
 * viewing (E12).
 *
 * <p>These run with nobody in the loop, which is what makes them a showcase for the method:
 * the person who takes the test can recompute the targets from the revealed seed and check
 * that none of them was chosen after their answer. What the module never exposes is the target
 * of an open trial, the seed of an open session, or a running score — see
 * {@link io.github.rogeriocisi.airlab.trials.SessionProgress}.
 *
 * <p>The data is anonymous by design and therefore exploratory, never confirmatory.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "trials",
        allowedDependencies = {"audit", "analysis", "support"})
package io.github.rogeriocisi.airlab.trials;
