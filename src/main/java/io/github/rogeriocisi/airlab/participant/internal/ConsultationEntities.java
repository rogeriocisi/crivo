package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.participant.ConsultationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A P4 consultation request, research side (V18): pseudonymous, with the pairing attributes and
 * the commitment. Everything but the state is fixed at creation, and the database refuses any
 * other change.
 */
@Entity
@Table(name = "consultation_request", schema = "research")
class ConsultationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Column(name = "sitter_code", nullable = false, length = 32, updatable = false)
    private String sitterCode;

    @Column(name = "questionnaire_version", nullable = false, length = 32, updatable = false)
    private String questionnaireVersion;

    @Column(name = "gender", nullable = false, length = 32, updatable = false)
    private String gender;

    @Column(name = "age_band", nullable = false, length = 32, updatable = false)
    private String ageBand;

    @Column(name = "death_period", nullable = false, length = 32, updatable = false)
    private String deathPeriod;

    @Column(name = "commitment", nullable = false, length = 64, updatable = false)
    private String commitment;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 24)
    private ConsultationState state;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private Instant lockedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected ConsultationRequestEntity() {
    }

    ConsultationRequestEntity(String code, String sitterCode, String questionnaireVersion, String gender,
                              String ageBand, String deathPeriod, String commitment, Instant lockedAt) {
        this.code = code;
        this.sitterCode = sitterCode;
        this.questionnaireVersion = questionnaireVersion;
        this.gender = gender;
        this.ageBand = ageBand;
        this.deathPeriod = deathPeriod;
        this.commitment = commitment;
        this.state = ConsultationState.LOCKED;
        this.lockedAt = lockedAt;
    }

    void withdraw(Instant at) {
        this.state = ConsultationState.WITHDRAWN;
        this.withdrawnAt = at;
    }

    void advance(ConsultationState next) {
        this.state = next;
    }

    String code() {
        return code;
    }

    String sitterCode() {
        return sitterCode;
    }

    String questionnaireVersion() {
        return questionnaireVersion;
    }

    String gender() {
        return gender;
    }

    String ageBand() {
        return ageBand;
    }

    String deathPeriod() {
        return deathPeriod;
    }

    String commitment() {
        return commitment;
    }

    ConsultationState state() {
        return state;
    }

    Instant lockedAt() {
        return lockedAt;
    }
}

/**
 * The sensitive part of a P4 request (V18): first name, answers and salt, encrypted. The only
 * change the database admits is erasing them, once, on withdrawal.
 */
@Entity
@Table(name = "consultation_detail", schema = "identity")
class ConsultationDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, length = 32, unique = true, updatable = false)
    private String requestCode;

    @Column(name = "first_name_enc", nullable = false)
    private String firstNameEnc;

    @Column(name = "answers_enc", nullable = false)
    private String answersEnc;

    @Column(name = "salt_enc", nullable = false)
    private String saltEnc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "erased_at")
    private Instant erasedAt;

    protected ConsultationDetailEntity() {
    }

    ConsultationDetailEntity(String requestCode, String firstNameEnc, String answersEnc, String saltEnc,
                             Instant createdAt) {
        this.requestCode = requestCode;
        this.firstNameEnc = firstNameEnc;
        this.answersEnc = answersEnc;
        this.saltEnc = saltEnc;
        this.createdAt = createdAt;
    }

    void erase(Instant at) {
        this.firstNameEnc = "";
        this.answersEnc = "";
        this.saltEnc = "";
        this.erasedAt = at;
    }

    String firstNameEnc() {
        return firstNameEnc;
    }

    String answersEnc() {
        return answersEnc;
    }

    String saltEnc() {
        return saltEnc;
    }

    boolean erased() {
        return erasedAt != null;
    }
}

interface ConsultationRequestRepository extends JpaRepository<ConsultationRequestEntity, Long> {

    Optional<ConsultationRequestEntity> findBySitterCodeAndStateNot(String sitterCode, ConsultationState state);

    Optional<ConsultationRequestEntity> findByCode(String code);

    List<ConsultationRequestEntity> findByCodeIn(Collection<String> codes);

    List<ConsultationRequestEntity> findByStateInOrderByLockedAtAscCodeAsc(Collection<ConsultationState> states);

    long countByStateNot(ConsultationState state);

    long countByState(ConsultationState state);
}

interface ConsultationDetailRepository extends JpaRepository<ConsultationDetailEntity, Long> {

    Optional<ConsultationDetailEntity> findByRequestCode(String requestCode);
}

/**
 * A first name released to the medium drawn for a pair (V19, SPEC E13, phase C). The record is
 * what lets this module refuse a name to anyone else: no release, no name. Revoked, never deleted.
 */
@Entity
@Table(name = "first_name_release", schema = "identity")
class FirstNameReleaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_code", nullable = false, length = 32, updatable = false)
    private String assignmentCode;

    @Column(name = "request_code", nullable = false, length = 32, updatable = false)
    private String requestCode;

    @Column(name = "medium_code", nullable = false, length = 32, updatable = false)
    private String mediumCode;

    @Column(name = "released_at", nullable = false, updatable = false)
    private Instant releasedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected FirstNameReleaseEntity() {
    }

    FirstNameReleaseEntity(String assignmentCode, String requestCode, String mediumCode, Instant releasedAt) {
        this.assignmentCode = assignmentCode;
        this.requestCode = requestCode;
        this.mediumCode = mediumCode;
        this.releasedAt = releasedAt;
    }

    void revoke(Instant at) {
        this.revokedAt = at;
    }

    String requestCode() {
        return requestCode;
    }
}

interface FirstNameReleaseRepository extends JpaRepository<FirstNameReleaseEntity, Long> {

    List<FirstNameReleaseEntity> findByAssignmentCodeAndRevokedAtIsNull(String assignmentCode);

    List<FirstNameReleaseEntity> findByAssignmentCodeAndMediumCodeAndRevokedAtIsNull(String assignmentCode,
                                                                                    String mediumCode);

    List<FirstNameReleaseEntity> findByRequestCodeAndRevokedAtIsNull(String requestCode);
}
