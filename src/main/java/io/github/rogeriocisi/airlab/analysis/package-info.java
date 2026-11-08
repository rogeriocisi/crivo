/**
 * Preregistered analysis and report (Epic E7).
 *
 * <p>Implemented so far: the exact one sided binomial test that is the primary analysis of
 * the paired protocol, validated against exact rational reference values in the test suite
 * (P9). The analysis run persistence, the secondary analyses and the report template are
 * the next slice; the table already exists (migration V9).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "analysis",
        allowedDependencies = {"audit", "support"})
package io.github.rogeriocisi.airlab.analysis;
