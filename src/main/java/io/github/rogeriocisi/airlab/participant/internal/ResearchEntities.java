package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.participant.ParticipantRole;
import io.github.rogeriocisi.airlab.participant.TargetRegistration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Pseudonymous rows of the {@code research} schema: codes and pairing attributes only. */
@Entity
@Table(name = "participant_role", schema = "research")
class ParticipantRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private ParticipantRole role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ParticipantRoleEntity() {
    }

    ParticipantRoleEntity(Long studyId, String code, ParticipantRole role) {
        this.studyId = studyId;
        this.code = code;
        this.role = role;
    }

    String code() {
        return code;
    }

    ParticipantRole role() {
        return role;
    }

    boolean active() {
        return active;
    }

    void deactivate() {
        this.active = false;
    }
}

@Entity
@Table(name = "target", schema = "research")
class TargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "sitter_code", nullable = false, length = 32)
    private String sitterCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 16)
    private TargetRegistration.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_band", nullable = false, length = 16)
    private TargetRegistration.AgeBand ageBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "death_period", nullable = false, length = 16)
    private TargetRegistration.DeathPeriod deathPeriod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TargetEntity() {
    }

    TargetEntity(Long studyId, String code, String sitterCode, TargetRegistration registration,
                 Instant createdAt) {
        this.studyId = studyId;
        this.code = code;
        this.sitterCode = sitterCode;
        this.gender = registration.gender();
        this.ageBand = registration.ageBand();
        this.deathPeriod = registration.deathPeriod();
        this.createdAt = createdAt;
    }

    Long studyId() {
        return studyId;
    }

    String code() {
        return code;
    }

    String sitterCode() {
        return sitterCode;
    }

    TargetRegistration.Gender gender() {
        return gender;
    }

    TargetRegistration.AgeBand ageBand() {
        return ageBand;
    }

    TargetRegistration.DeathPeriod deathPeriod() {
        return deathPeriod;
    }
}

@Entity
@Table(name = "pair", schema = "research")
class PairEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "target_a", nullable = false, length = 32)
    private String targetA;

    @Column(name = "target_b", nullable = false, length = 32)
    private String targetB;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PairEntity() {
    }

    PairEntity(Long studyId, String code, String targetA, String targetB, Instant createdAt) {
        this.studyId = studyId;
        this.code = code;
        this.targetA = targetA;
        this.targetB = targetB;
        this.createdAt = createdAt;
    }

    String code() {
        return code;
    }

    String targetA() {
        return targetA;
    }

    String targetB() {
        return targetB;
    }
}
