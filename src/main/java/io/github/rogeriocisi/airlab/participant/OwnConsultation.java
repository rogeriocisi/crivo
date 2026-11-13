package io.github.rogeriocisi.airlab.participant;

import java.time.Instant;
import java.util.Map;

/**
 * A P4 request with its content, built for its sitter alone: the P4 visibility matrix gives
 * the sitter their own answers and nobody else's (SPEC E13).
 */
public record OwnConsultation(String code,
                              String questionnaireVersion,
                              String firstName,
                              Map<String, String> answers,
                              String commitment,
                              Instant lockedAt) {

    public OwnConsultation {
        answers = Map.copyOf(answers);
    }

    @Override
    public String toString() {
        return "OwnConsultation[code=" + code + ", redacted]";
    }
}
