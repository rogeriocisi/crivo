package io.github.rogeriocisi.airlab.participant;

/**
 * Pseudonymous target projection: everything the pairing and the draw are allowed to see,
 * and nothing else. By construction it carries no name (SPEC section 4, architectural
 * rule 3: a forbidden field is absent from the projection, not filtered in the view).
 */
public record TargetProfile(String targetCode,
                            String sitterCode,
                            TargetRegistration.Gender gender,
                            TargetRegistration.AgeBand ageBand,
                            TargetRegistration.DeathPeriod deathPeriod) {
}
