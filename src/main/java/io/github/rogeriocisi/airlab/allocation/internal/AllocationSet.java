package io.github.rogeriocisi.airlab.allocation.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The stored draw. Immutable after the draw, except for the single moment the seed is
 * revealed; migration V5 enforces that with a trigger.
 *
 * <p>The seed is written at draw time, but encrypted ({@code seedEscrow}), and the plain
 * value appears in {@code seedHex} only at the unblinding. So a database dump taken while
 * the study is running does not break the commitment scheme, and the seed still survives a
 * restart, which an in-memory secret would not (P5).
 */
@Entity
@Table(name = "allocation_set", schema = "research")
class AllocationSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "seed_commitment", nullable = false, length = 64)
    private String seedCommitment;

    @Column(name = "draw_input", nullable = false)
    private String drawInput;

    @Column(name = "seed_escrow", nullable = false)
    private String seedEscrow;

    @Column(name = "allocations", nullable = false)
    private String allocations;

    @Column(name = "drawn_at", nullable = false)
    private Instant drawnAt;

    @Column(name = "seed_hex", length = 64)
    private String seedHex;

    @Column(name = "seed_revealed_at")
    private Instant seedRevealedAt;

    protected AllocationSet() {
    }

    AllocationSet(Long studyId, String seedCommitment, String drawInput, String allocations,
                  String seedEscrow, Instant drawnAt) {
        this.studyId = studyId;
        this.seedCommitment = seedCommitment;
        this.drawInput = drawInput;
        this.allocations = allocations;
        this.seedEscrow = seedEscrow;
        this.drawnAt = drawnAt;
    }

    void reveal(String plainSeedHex, Instant at) {
        if (this.seedHex != null) {
            throw new IllegalStateException("seed already revealed for study " + studyId);
        }
        this.seedHex = plainSeedHex;
        this.seedRevealedAt = at;
    }

    Long studyId() {
        return studyId;
    }

    String seedCommitment() {
        return seedCommitment;
    }

    String drawInput() {
        return drawInput;
    }

    String allocations() {
        return allocations;
    }

    String seedEscrow() {
        return seedEscrow;
    }

    Instant drawnAt() {
        return drawnAt;
    }

    String seedHex() {
        return seedHex;
    }

    Instant seedRevealedAt() {
        return seedRevealedAt;
    }
}
