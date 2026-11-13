package io.github.rogeriocisi.airlab.allocation;

import java.util.List;
import java.util.Optional;

/**
 * The draw as an operation of the study (Epic E3).
 *
 * <p>Note what this API deliberately does <em>not</em> offer: a way for the researcher to
 * read the plan or the seed. The blinding matrix (SPEC, section 4) forbids the researcher
 * from knowing the reading-to-target link before the unblinding, so the plan leaves this
 * module only as an {@link AllocationDrawn} event, consumed by the session module to set
 * up sessions, and the seed is readable only after {@link #revealSeed}.
 */
public interface AllocationService {

    /**
     * Draws the allocations, stores them with the commitment and emits the audit event.
     *
     * @return the published commitment, which goes into the preregistration
     */
    String draw(long studyId, DrawInput input);

    /**
     * P4 (SPEC E13, phase C): commits to a fresh seed for a study whose draws happen during
     * collection, and returns the commitment that goes into the frozen preregistration. The seed
     * stays in encrypted custody, exactly as for the P1 draw, and leaves only through
     * {@link #revealSeed}.
     */
    String commitSeed(long studyId, String studyCode);

    /**
     * P4: the medium drawn for a pair on an attempt, derived from the escrowed seed without
     * exposing it. The caller records the pool, so the draw can be redone once the seed is out.
     */
    String drawStructuredMedium(long studyId, String pairCode, int attempt, List<String> eligibleMediums);

    /** P4: the order in which the drawn medium sees the two requests of a pair. */
    List<String> structuredNameOrder(long studyId, String pairCode, int attempt, List<String> requestCodes);

    /** The published commitment of a study, if the draw already happened. */
    Optional<String> commitment(long studyId);

    /**
     * Reveals the seed. Only the unblinding module calls this, and only after every
     * scoring packet is locked (Constitution P5, architectural rule 5).
     *
     * @return the seed in hex, so it can go into the verification package
     */
    String revealSeed(long studyId, String actorCode);

    /** Snapshot for the verification CLI and the verification package (P7, Epic E8/E9). */
    Optional<AllocationSnapshot> snapshot(long studyId);

    /**
     * Order in which the items of a redacted reading are shown to the sitter, so that a
     * revealing item cannot colour the whole list (Epic E6).
     *
     * <p>Uses the escrowed seed internally and never returns it: the order is available during
     * the study, while the seed stays sealed until the unblinding (P1, P5).
     */
    List<Integer> itemPresentationOrder(long studyId, String redactedReadingCode, int itemCount);

    /**
     * Which items to keep so that the two readings of a pair reach the sitter with comparable
     * length, when the difference exceeds what the preregistration allows (Epic E5).
     *
     * <p>Also seed-derived and also without exposing the seed, so the cut is reproducible by
     * an auditor afterwards and is made by nobody who has read the content.
     *
     * @param keep how many items to keep, decided by the preregistered tolerance
     */
    List<Integer> balancedItemSelection(long studyId, String redactedReadingCode,
                                        int itemCount, int keep);
}
