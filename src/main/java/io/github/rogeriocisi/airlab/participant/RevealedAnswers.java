package io.github.rogeriocisi.airlab.participant;

import java.util.Map;

/**
 * A sitter's answers after the unblinding (SPEC E13, phase D), for the scoring and nothing else.
 * Handed out only for a request in {@link ConsultationState#UNBLINDED}; before that, the answers
 * do not leave this module.
 *
 * @param commitmentVerified whether the salted commitment published when the request was locked
 *                           still matches the stored request — checked here, where the first name
 *                           and the salt are
 */
public record RevealedAnswers(String requestCode,
                              String questionnaireVersion,
                              Map<String, String> answers,
                              boolean commitmentVerified) {

    public RevealedAnswers {
        answers = Map.copyOf(answers);
    }

    @Override
    public String toString() {
        return "RevealedAnswers[request=" + requestCode + ", verified=" + commitmentVerified + ", redacted]";
    }
}
