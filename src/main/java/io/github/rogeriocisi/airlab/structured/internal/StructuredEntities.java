package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.structured.AssignmentState;
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

/** What a P4 study froze and the cycle reads (V19). Immutable: the database refuses any change. */
@Entity
@Table(name = "structured_study", schema = "research")
class StructuredStudyEntity {

    @Id
    @Column(name = "study_id")
    private Long studyId;

    @Column(name = "questionnaire_version", nullable = false, length = 32, updatable = false)
    private String questionnaireVersion;

    @Column(name = "target_pairs", nullable = false, updatable = false)
    private int targetPairs;

    @Column(name = "deadline_days", nullable = false, updatable = false)
    private int deadlineDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "max_pairs_per_medium", updatable = false)
    private Integer maxPairsPerMedium;

    protected StructuredStudyEntity() {
    }

    StructuredStudyEntity(Long studyId, String questionnaireVersion, int targetPairs, int deadlineDays,
                          int maxPairsPerMedium, Instant createdAt) {
        this.studyId = studyId;
        this.questionnaireVersion = questionnaireVersion;
        this.targetPairs = targetPairs;
        this.deadlineDays = deadlineDays;
        this.maxPairsPerMedium = maxPairsPerMedium;
        this.createdAt = createdAt;
    }

    /** Null for a study frozen before the cap existed: no cap. */
    Integer maxPairsPerMedium() {
        return maxPairsPerMedium;
    }

    Long studyId() {
        return studyId;
    }

    String questionnaireVersion() {
        return questionnaireVersion;
    }

    int targetPairs() {
        return targetPairs;
    }

    int deadlineDays() {
        return deadlineDays;
    }
}

/** Two locked requests of the same stratum (V19). Dissolved, never deleted. */
@Entity
@Table(name = "structured_pair", schema = "research")
class StructuredPairEntity {

    static final String DISSOLVED_BY_WITHDRAWAL = "WITHDRAWAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Column(name = "study_id", nullable = false, updatable = false)
    private Long studyId;

    @Column(name = "request_a", nullable = false, length = 32, updatable = false)
    private String requestA;

    @Column(name = "request_b", nullable = false, length = 32, updatable = false)
    private String requestB;

    @Column(name = "formed_at", nullable = false, updatable = false)
    private Instant formedAt;

    @Column(name = "dissolved_at")
    private Instant dissolvedAt;

    @Column(name = "dissolution_reason", length = 32)
    private String dissolutionReason;

    protected StructuredPairEntity() {
    }

    StructuredPairEntity(String code, Long studyId, String requestA, String requestB, Instant formedAt) {
        this.code = code;
        this.studyId = studyId;
        this.requestA = requestA;
        this.requestB = requestB;
        this.formedAt = formedAt;
    }

    void dissolve(Instant at, String reason) {
        this.dissolvedAt = at;
        this.dissolutionReason = reason;
    }

    String code() {
        return code;
    }

    Long studyId() {
        return studyId;
    }

    String requestA() {
        return requestA;
    }

    String requestB() {
        return requestB;
    }

    List<String> requests() {
        return List.of(requestA, requestB);
    }

    boolean active() {
        return dissolvedAt == null;
    }
}

/** A pair sent to a drawn medium (V19). Closed once — completed, expired or cancelled. */
@Entity
@Table(name = "reading_assignment", schema = "research")
class ReadingAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Column(name = "pair_code", nullable = false, length = 32, updatable = false)
    private String pairCode;

    @Column(name = "attempt", nullable = false, updatable = false)
    private int attempt;

    @Column(name = "medium_code", nullable = false, length = 32, updatable = false)
    private String mediumCode;

    @Column(name = "pool", nullable = false, updatable = false)
    private String pool;

    @Column(name = "pool_sha256", nullable = false, length = 64, updatable = false)
    private String poolSha256;

    @Column(name = "first_request", nullable = false, length = 32, updatable = false)
    private String firstRequest;

    @Column(name = "second_request", nullable = false, length = 32, updatable = false)
    private String secondRequest;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private AssignmentState state;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected ReadingAssignmentEntity() {
    }

    ReadingAssignmentEntity(String code, String pairCode, int attempt, String mediumCode, String pool,
                            String poolSha256, String firstRequest, String secondRequest, Instant assignedAt,
                            Instant dueAt) {
        this.code = code;
        this.pairCode = pairCode;
        this.attempt = attempt;
        this.mediumCode = mediumCode;
        this.pool = pool;
        this.poolSha256 = poolSha256;
        this.firstRequest = firstRequest;
        this.secondRequest = secondRequest;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
        this.state = AssignmentState.OPEN;
    }

    void close(AssignmentState next, Instant at) {
        this.state = next;
        this.closedAt = at;
    }

    String code() {
        return code;
    }

    String pairCode() {
        return pairCode;
    }

    int attempt() {
        return attempt;
    }

    String mediumCode() {
        return mediumCode;
    }

    /** The request shown at a position: 1 is the first drawn, 2 the second. */
    String requestAt(int position) {
        return switch (position) {
            case 1 -> firstRequest;
            case 2 -> secondRequest;
            default -> throw new IllegalArgumentException("a pair has positions 1 and 2");
        };
    }

    Instant assignedAt() {
        return assignedAt;
    }

    Instant dueAt() {
        return dueAt;
    }

    AssignmentState state() {
        return state;
    }

    boolean open() {
        return state == AssignmentState.OPEN;
    }
}

/** A medium's locked reading for one name of a pair (V19). Born locked; the database refuses changes. */
@Entity
@Table(name = "structured_reading", schema = "research")
class StructuredReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Column(name = "assignment_code", nullable = false, length = 32, updatable = false)
    private String assignmentCode;

    @Column(name = "slot", nullable = false, updatable = false)
    private short slot;

    @Column(name = "request_code", nullable = false, length = 32, updatable = false)
    private String requestCode;

    @Column(name = "questionnaire_version", nullable = false, length = 32, updatable = false)
    private String questionnaireVersion;

    @Column(name = "answers_enc", nullable = false, updatable = false)
    private String answersEnc;

    @Column(name = "salt_enc", nullable = false, updatable = false)
    private String saltEnc;

    @Column(name = "commitment", nullable = false, length = 64, updatable = false)
    private String commitment;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private Instant lockedAt;

    protected StructuredReadingEntity() {
    }

    StructuredReadingEntity(String code, String assignmentCode, int slot, String requestCode,
                            String questionnaireVersion, String answersEnc, String saltEnc, String commitment,
                            Instant lockedAt) {
        this.code = code;
        this.assignmentCode = assignmentCode;
        this.slot = (short) slot;
        this.requestCode = requestCode;
        this.questionnaireVersion = questionnaireVersion;
        this.answersEnc = answersEnc;
        this.saltEnc = saltEnc;
        this.commitment = commitment;
        this.lockedAt = lockedAt;
    }

    String code() {
        return code;
    }

    int slot() {
        return slot;
    }

    String requestCode() {
        return requestCode;
    }

    String answersEnc() {
        return answersEnc;
    }

    String saltEnc() {
        return saltEnc;
    }

    String commitment() {
        return commitment;
    }

    Instant lockedAt() {
        return lockedAt;
    }
}

interface StructuredStudyRepository extends JpaRepository<StructuredStudyEntity, Long> {
}

interface StructuredPairRepository extends JpaRepository<StructuredPairEntity, Long> {

    List<StructuredPairEntity> findByStudyIdAndDissolvedAtIsNullOrderByCodeAsc(Long studyId);

    List<StructuredPairEntity> findByStudyIdOrderByCodeAsc(Long studyId);

    long countByStudyIdAndDissolvedAtIsNull(Long studyId);

    long countByStudyIdAndDissolvedAtIsNotNull(Long studyId);

    Optional<StructuredPairEntity> findByCode(String code);

    @org.springframework.data.jpa.repository.Query("select p from StructuredPairEntity p where p.dissolvedAt is null "
            + "and (p.requestA = :request or p.requestB = :request)")
    List<StructuredPairEntity> findActiveByRequest(@org.springframework.data.repository.query.Param("request") String request);
}

interface ReadingAssignmentRepository extends JpaRepository<ReadingAssignmentEntity, Long> {

    Optional<ReadingAssignmentEntity> findByCode(String code);

    List<ReadingAssignmentEntity> findByPairCodeOrderByAttemptAsc(String pairCode);

    List<ReadingAssignmentEntity> findByPairCodeInOrderByPairCodeAscAttemptAsc(Collection<String> pairCodes);

    List<ReadingAssignmentEntity> findByStateOrderByCodeAsc(AssignmentState state);

    List<ReadingAssignmentEntity> findByMediumCodeAndStateInOrderByAssignedAtDescCodeAsc(String mediumCode,
                                                                                       Collection<AssignmentState> states);

    long countByMediumCodeAndStateIn(String mediumCode, Collection<AssignmentState> states);
}

interface StructuredReadingRepository extends JpaRepository<StructuredReadingEntity, Long> {

    List<StructuredReadingEntity> findByAssignmentCodeOrderBySlotAsc(String assignmentCode);

    List<StructuredReadingEntity> findByAssignmentCodeIn(Collection<String> assignmentCodes);
}
