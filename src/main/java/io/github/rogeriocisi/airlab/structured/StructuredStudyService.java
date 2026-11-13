package io.github.rogeriocisi.airlab.structured;

import java.time.Instant;
import java.util.Optional;

/**
 * The P4 study and its automatic cycle (SPEC E13, phase C), for the administrator and for the
 * server's scheduler.
 *
 * <p>Note what this interface does not offer: no method returns which medium has which pair,
 * what a reading says, or anything computed from a reading. The administrator gets the frozen
 * document and counts; the link between pair and medium is blind until the unblinding.
 */
public interface StructuredStudyService {

    /** Protocol tag stored with the study. */
    String PROTOCOL = "STRUCTURED_READING_V1";

    /**
     * Checks the terms without storing anything, so the review page can show errors first.
     *
     * @throws IllegalArgumentException with a message in Portuguese, for the administrator
     */
    void check(StructuredStudyTerms terms);

    /**
     * Whether a questionnaire still marked as a draft ({@code -rascunho}) may be frozen here: only
     * in the local profile, where the data are fictitious (SPEC E13, phase C).
     */
    boolean draftQuestionnaireAllowed();

    /**
     * Creates the P4 study, commits to its seed and freezes the preregistration — one step, with
     * no unfrozen version in the database — and then runs the first cycle, so the requests already
     * waiting are paired at once.
     *
     * @throws IllegalArgumentException when the terms are invalid
     * @throws IllegalStateException    when a P4 study is already open, or the questionnaire is a
     *                                  draft where drafts are not allowed
     */
    StructuredStudyView open(StructuredStudyTerms terms, String actorCode);

    /** The open P4 study, if there is one. */
    Optional<StructuredStudyView> current();

    /** P4 studies already unblinded, newest first (SPEC E13, phase D). */
    java.util.List<StructuredStudyView> concluded();

    /** A P4 study by its code, open or concluded. */
    Optional<StructuredStudyView> byCode(String studyCode);

    /**
     * The report of a P4 study. Empty for a study still collecting: the report is written at the
     * unblinding and the database refuses it before (E10, lock 1).
     */
    Optional<StructuredReport> report(String studyCode);

    /** Counts of the open study; {@link StructuredProgress#none()} when there is none. */
    StructuredProgress progress();

    /**
     * One run of the automatic cycle, in this order: dissolve pairs with a withdrawn request;
     * expire requests to a medium past their deadline; cancel requests whose medium closed the
     * account; form pairs; send the pairs that have no medium; and, when the N pairs have both
     * readings, unblind, score, analyse and report (phase D). Running it twice in a row changes
     * nothing the second time.
     *
     * @param now the moment the deadlines are checked against
     */
    CycleReport runCycle(Instant now);
}
