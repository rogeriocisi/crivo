package io.github.rogeriocisi.airlab.participant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * What a sitter registers about the deceased target: the first name, which the medium will
 * hear, plus the pseudonymous attributes the pairing needs. Anything else is optional and
 * exists only for the debriefing (SPEC, Epic E2).
 *
 * @param firstName identifiable, stored encrypted in the {@code identity} schema
 * @param notes     optional, for the debriefing only; never reaches research data
 */
public record TargetRegistration(@NotBlank String firstName,
                                 @NotNull Gender gender,
                                 @NotNull AgeBand ageBand,
                                 @NotNull DeathPeriod deathPeriod,
                                 String notes) {

    @Override
    public String toString() {
        return "TargetRegistration(redacted)";
    }

    public enum Gender { FEMININE, MASCULINE, OTHER, UNDECLARED }

    /** Age at death, in bands: a band pairs well and identifies much less than a date. */
    public enum AgeBand { UNDER_18, FROM_18_TO_39, FROM_40_TO_59, FROM_60_TO_79, FROM_80 }

    /** How long ago the death occurred, in bands, for the same reason. */
    public enum DeathPeriod { UNDER_1_YEAR, FROM_1_TO_5_YEARS, FROM_5_TO_15_YEARS, OVER_15_YEARS }
}
