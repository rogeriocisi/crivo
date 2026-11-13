package io.github.rogeriocisi.airlab.participant;

import java.time.Instant;

/**
 * A P4 request as the pairing sees it (SPEC E13, phase C): codes, questionnaire version, the
 * three pairing attributes and where the request stands. No first name and no scored answer —
 * the pairing needs neither, so they are absent rather than hidden.
 *
 * <p>For the system's own use. No administrator screen renders it: the pairing attributes are
 * answers of the sitter, and the P4 matrix gives the administrator none of them.
 */
public record ConsultationRef(String code,
                              String sitterCode,
                              String questionnaireVersion,
                              String gender,
                              String ageBand,
                              String deathPeriod,
                              ConsultationState state,
                              Instant lockedAt) {

    /** The pairing stratum: same gender, same age band, same period of death. */
    public String stratum() {
        return gender + "|" + ageBand + "|" + deathPeriod;
    }
}
