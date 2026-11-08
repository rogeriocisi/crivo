/**
 * Turning a transcript into numbered, verifiable items (Epic E5).
 *
 * <p>Implemented so far: {@link io.github.rogeriocisi.airlab.redaction.ClueScrubber}, the
 * guard that keeps the target's first name and session clues out of the item list. The
 * persistence and the redactor screens are the next slice of the MVP; the tables already
 * exist (migration V7).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "redaction",
        allowedDependencies = {"audit", "support"})
package io.github.rogeriocisi.airlab.redaction;
