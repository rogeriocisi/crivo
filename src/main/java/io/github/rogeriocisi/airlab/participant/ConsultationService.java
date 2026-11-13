package io.github.rogeriocisi.airlab.participant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    // --- Phase C: what the P4 flow needs from the requests ------------------------------------

    /** Requests in the given states, oldest lock first (code breaks ties), for the pairing. */
    List<ConsultationRef> requests(Set<ConsultationState> states);

    /** The requests with these codes; an unknown code is simply absent. */
    List<ConsultationRef> requestsByCode(Collection<String> codes);

    /**
     * Moves a request along the P4 flow. Refused unless the request is in {@code from} and the
     * step is one the flow has: pairing ({@code LOCKED → PAIRED}), sending
     * ({@code PAIRED → SENT_TO_MEDIUM}), both readings in ({@code SENT_TO_MEDIUM → READING_RECEIVED}),
     * an expired or cancelled send ({@code SENT_TO_MEDIUM → PAIRED}), a dissolved pair (back to
     * {@code LOCKED}) and the unblinding ({@code READING_RECEIVED → UNBLINDED}). The step itself is audited by the operation that causes it.
     */
    void advance(String requestCode, ConsultationState from, ConsultationState to);

    /**
     * Releases the first names of a pair's requests to the medium drawn for them (SPEC E13, phase
     * C). Recorded in {@code identity.first_name_release} and audited without the medium's code,
     * because the trail is public and the link between medium and pair is blind until the
     * unblinding.
     *
     * @throws IllegalStateException when the medium is not an active, confirmed médium, or a
     *                               request is withdrawn
     */
    void releaseFirstNames(String assignmentCode, String mediumCode, List<String> requestCodes);

    /** Revokes every release of an assignment: expired, cancelled or dissolved. */
    void revokeFirstNames(String assignmentCode);

    /**
     * The first names released to this medium under this assignment, by request code. A release
     * that does not exist, was revoked or belongs to another medium returns nothing.
     */
    Map<String, String> releasedFirstNames(String mediumCode, String assignmentCode);

    // --- Phase D: after the unblinding ---------------------------------------------------------

    /**
     * The answers of requests already unblinded ({@link ConsultationState#UNBLINDED}), each with
     * its commitment checked against the stored request and salt (SPEC E13, phase D). A request
     * in any other state is simply absent: before the unblinding, the answers stay here.
     */
    Map<String, RevealedAnswers> revealedAnswers(Collection<String> requestCodes);
}
