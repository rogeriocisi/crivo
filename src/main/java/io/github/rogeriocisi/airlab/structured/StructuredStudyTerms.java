package io.github.rogeriocisi.airlab.structured;

import java.io.Serializable;

/**
 * What the administrator writes into a P4 preregistration (SPEC E13, phase C). Everything else —
 * the questionnaire, the pairing rule, the scoring rule, the analysis, the exclusion rules — is
 * fixed by the protocol and goes into the frozen document as it is.
 *
 * <p>Serializable because the terms wait in the HTTP session while the administrator reviews
 * them: as with the sitter's request, the database receives the study once, already frozen.
 *
 * @param title                   study title
 * @param targetPairs             N: the study stops pairing at this many active pairs, and the
 *                                unblinding (phase D) comes when they all have both readings
 * @param deadlineDays            days the medium has to lock both readings of a pair
 * @param expectedEffect          expected standardised difference d, for the declared power
 * @param hypothesis              the hypothesis, in the researcher's words
 * @param mediumInclusionCriteria who may read; declared because selecting mediums on past
 *                                performance inflates the effect
 */
public record StructuredStudyTerms(String title,
                                   int targetPairs,
                                   int deadlineDays,
                                   double expectedEffect,
                                   String hypothesis,
                                   String mediumInclusionCriteria) implements Serializable {

    public static final int DEFAULT_DEADLINE_DAYS = 14;
    public static final int MAXIMUM_DEADLINE_DAYS = 90;
    public static final int MAXIMUM_TARGET_PAIRS = 10_000;

    /** The form's starting point: d = 0.5 and the 29 pairs it needs (rounded up to 30). */
    public static StructuredStudyTerms defaults() {
        return new StructuredStudyTerms(
                "Leitura estruturada automatizada — estudo 1",
                30,
                DEFAULT_DEADLINE_DAYS,
                0.5,
                "As leituras dos médiuns coincidem mais com as respostas do consulente-alvo do que com as "
                        + "do consulente-chamariz do mesmo par (d > 0).",
                "Qualquer conta de médium com e-mail confirmado, sem triagem por desempenho anterior.");
    }
}
