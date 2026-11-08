package io.github.rogeriocisi.airlab.allocation;

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
}
