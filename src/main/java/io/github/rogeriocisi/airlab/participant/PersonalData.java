package io.github.rogeriocisi.airlab.participant;

import jakarta.validation.constraints.NotBlank;

/**
 * Identifiable data, kept to the minimum the study actually needs (LGPD, P6).
 *
 * <p>Instances exist only inside the participant module and inside the request that created
 * them. They are never logged, never put in an exception message and never serialised into
 * an audit payload.
 */
public record PersonalData(@NotBlank String fullName, String contact) {

    @Override
    public String toString() {
        // Overridden so that an accidental log statement cannot leak personal data (P6).
        return "PersonalData(redacted)";
    }
}
