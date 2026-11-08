package io.github.rogeriocisi.airlab.participant;

import java.util.List;

/**
 * People, consent and targets (Epic E2, Constitution P6).
 *
 * <p>This is the only module that touches the {@code identity} schema. Everything it hands
 * to other modules is pseudonymous, with one deliberate exception: {@link #releaseFirstName},
 * the single audited path by which the target's first name reaches the experimenter who is
 * about to conduct a session (SPEC section 4).
 */
public interface ParticipantService {

    /**
     * Enrols a person and returns their pseudonymous code. The name and contact are stored
     * encrypted in the {@code identity} schema and never leave this module.
     */
    String enrol(long studyId, ParticipantRole role, PersonalData personalData,
                 ConsentDeclaration consent, String actorCode);

    /** Registers the deceased target a sitter indicates, and returns the target code. */
    String registerTarget(long studyId, String sitterCode, TargetRegistration registration,
                          String actorCode);

    /**
     * Releases the target's first name to the experimenter of a session, and records the
     * release in the audit trail. Nothing else about the target is returned, ever.
     */
    String releaseFirstName(String targetCode, String experimenterCode, String sessionCode);

    /** Pseudonymous target profiles, which is all the pairing needs (Epic E3). */
    List<TargetProfile> targets(long studyId);

    /**
     * Forms the pairs from the preregistered criteria: same gender and same age band, two
     * targets of different sitters. Returns the pairs formed.
     */
    List<PairView> formPairs(long studyId, String actorCode);

    List<PairView> pairs(long studyId);

    /** Codes of the people holding a role in a study, for the draw. */
    List<String> codesWithRole(long studyId, ParticipantRole role);

    /**
     * Withdrawal at any moment, without justification (P6). Identifiable data is erased and
     * the event is recorded; the pseudonymous research rows stay, marked inactive.
     */
    void withdraw(String participantCode, String actorCode);
}
