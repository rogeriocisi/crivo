package io.github.rogeriocisi.airlab.participant;

import java.util.Optional;

/**
 * P4 consultation requests (SPEC E13, phase B).
 *
 * <p>In {@code participant} because the first name and the answers are sensitive data that live
 * in the {@code identity} schema. What leaves here for anyone but the sitter is pseudonymous:
 * a code, a state, a questionnaire version and a commitment.
 */
public interface ConsultationService {

    int MAXIMUM_FIRST_NAME_LENGTH = 60;

    /**
     * Checks a draft without storing anything, so the review page can show errors before the
     * sitter confirms.
     *
     * @throws IllegalArgumentException with a message in Portuguese, for the person filling in
     */
    void check(ConsultationDraft draft);

    /**
     * Locks a request: stores it encrypted, publishes the salted commitment in the audit trail
     * and notifies the administrator. There is no unlocked state in the database.
     *
     * @throws IllegalArgumentException when the draft is invalid
     * @throws IllegalStateException    when the account is not an active, confirmed consulente,
     *                                  or already has an active request
     */
    ConsultationStatus submit(String sitterCode, ConsultationDraft draft);

    /** The sitter's active request, without content. */
    Optional<ConsultationStatus> status(String sitterCode);

    /** The sitter's active request with its content, for that sitter alone. */
    Optional<OwnConsultation> own(String sitterCode);

    /** Requests by situation, for the administrator. */
    ConsultationCounts counts();
}
