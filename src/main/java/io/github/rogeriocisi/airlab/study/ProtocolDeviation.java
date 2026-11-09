package io.github.rogeriocisi.airlab.study;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * A departure of the execution from the frozen plan (Constitution P2, Nosek et al., 2018).
 *
 * <p>Distinct from an amendment: an amendment changes the plan and creates a new
 * preregistration version, while a deviation says the study did not follow the version it
 * froze. Recording it is not an admission that spoils the study; it is what keeps the study
 * interpretable. A preregistration with transparently reported deviations gives more
 * confidence than no preregistration at all.
 *
 * <p>Append-only, enforced by the database (migration V12), and the report has to show every
 * deviation next to the result.
 */
public record ProtocolDeviation(long studyId,
                                int preregistrationVersion,
                                @NotNull Kind kind,
                                @NotBlank String description,
                                @NotNull Instant occurredAt,
                                Instant recordedAt,
                                String recordedBy) {

    public enum Kind {
        /** Collection stopped short of, or went past, the preregistered number of units. */
        SAMPLE_SIZE,
        /** An exclusion the plan did not anticipate. */
        EXCLUSION,
        /** Sessions ran outside the counterbalanced schedule. */
        SCHEDULE,
        /** A blinding rule could not be held, including the experimenter/redactor split. */
        BLINDING,
        /** Scoring departed from the preregistered scale or procedure. */
        SCORING,
        /** The analysis differed from the declared one. */
        ANALYSIS,
        OTHER
    }
}
