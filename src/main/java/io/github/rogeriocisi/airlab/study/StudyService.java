package io.github.rogeriocisi.airlab.study;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

/**
 * Study and preregistration (Epic E1).
 */
public interface StudyService {

    /** Creates a study in {@link StudyStatus#DRAFT} with version 1 of the preregistration. */
    StudySummary create(@NotBlank String title, @Valid PreregistrationPlan plan, String actorCode);

    /**
     * Freezes the current preregistration version together with the draw commitment, and
     * returns the document hash to publish (P2, P5).
     *
     * @param allocationCommitment commitment returned by the draw, bound into the document
     */
    PreregistrationSnapshot freeze(long studyId, String allocationCommitment, String actorCode);

    /**
     * Creates a new version from an amendment. The previous version stays visible and its
     * hash stays valid; the reason is mandatory (P2).
     */
    PreregistrationSnapshot amend(long studyId, @Valid PreregistrationPlan plan,
                                  @NotBlank String reason, String actorCode);

    void advanceStatus(long studyId, StudyStatus target, String actorCode);

    Optional<StudySummary> byId(long studyId);

    Optional<StudySummary> byCode(String code);

    List<StudySummary> all();

    /** Latest version, frozen or not. */
    Optional<PreregistrationSnapshot> currentPreregistration(long studyId);

    /** Every version, oldest first: an amendment never hides what came before it. */
    List<PreregistrationSnapshot> preregistrationHistory(long studyId);
}
