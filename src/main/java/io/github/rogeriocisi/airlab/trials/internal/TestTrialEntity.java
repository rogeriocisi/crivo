package io.github.rogeriocisi.airlab.trials.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One trial. The target position is written at lock time and never before: migration V15
 * enforces that with a check constraint, and a trigger keeps a locked trial immutable.
 */
@Entity
@Table(name = "test_trial", schema = "research")
class TestTrialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_code", nullable = false, length = 32)
    private String sessionCode;

    @Column(name = "trial_index", nullable = false)
    private int trialIndex;

    /** Pool slugs in presentation order, comma separated; null for the forced choice. */
    @Column(name = "candidate_slugs")
    private String candidateSlugs;

    @Column(name = "impression")
    private String impression;

    @Column(name = "impression_sha256", length = 64)
    private String impressionSha256;

    @Column(name = "impression_locked_at")
    private Instant impressionLockedAt;

    @Column(name = "chosen_position")
    private Integer chosenPosition;

    /** Presentation positions in order of similarity, comma separated; remote viewing only. */
    @Column(name = "ranked_positions")
    private String rankedPositions;

    @Column(name = "target_position")
    private Integer targetPosition;

    @Column(name = "hit")
    private Boolean hit;

    @Column(name = "locked_at")
    private Instant lockedAt;

    protected TestTrialEntity() {
    }

    TestTrialEntity(String sessionCode, int trialIndex) {
        this.sessionCode = sessionCode;
        this.trialIndex = trialIndex;
    }

    void lockImpression(String text, String sha256, String candidateSlugsInOrder, Instant at) {
        if (this.impressionLockedAt != null) {
            throw new IllegalStateException(
                    "the impression of trial " + trialIndex + " is already locked");
        }
        this.impression = text;
        this.impressionSha256 = sha256;
        this.candidateSlugs = candidateSlugsInOrder;
        this.impressionLockedAt = at;
    }

    void lockAnswer(Integer chosen, String ranking, int target, Instant at) {
        if (this.lockedAt != null) {
            throw new IllegalStateException("trial " + trialIndex + " is already locked");
        }
        this.chosenPosition = chosen;
        this.rankedPositions = ranking;
        this.targetPosition = target;
        this.hit = chosen != null && chosen == target;
        this.lockedAt = at;
    }

    String sessionCode() {
        return sessionCode;
    }

    int trialIndex() {
        return trialIndex;
    }

    String candidateSlugs() {
        return candidateSlugs;
    }

    String impression() {
        return impression;
    }

    String impressionSha256() {
        return impressionSha256;
    }

    Instant impressionLockedAt() {
        return impressionLockedAt;
    }

    Integer chosenPosition() {
        return chosenPosition;
    }

    String rankedPositions() {
        return rankedPositions;
    }

    Integer targetPosition() {
        return targetPosition;
    }

    Boolean hit() {
        return hit;
    }

    Instant lockedAt() {
        return lockedAt;
    }

    boolean locked() {
        return lockedAt != null;
    }
}
