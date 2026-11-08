package io.github.rogeriocisi.airlab.participant;

import jakarta.validation.constraints.NotBlank;

/**
 * Consent as given, with the version of the term the person actually read (P6).
 *
 * @param sensitiveDataAccepted specific, highlighted consent for sensitive data: religious
 *                              conviction, health and information about the deceased
 */
public record ConsentDeclaration(@NotBlank String termVersion, boolean sensitiveDataAccepted) {

    public ConsentDeclaration {
        if (!sensitiveDataAccepted) {
            // The study cannot proceed on general consent alone (LGPD, art. 11).
            throw new IllegalArgumentException("specific consent for sensitive data is required");
        }
    }
}
