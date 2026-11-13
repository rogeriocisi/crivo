package io.github.rogeriocisi.airlab.structured.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The score of one P4 pair (V20). Written once, at the unblinding; the database refuses it for a
 * study that is not unblinded and refuses any change afterwards.
 */
@Entity
@Table(name = "structured_score", schema = "research")
class StructuredScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false, updatable = false)
    private Long studyId;

    @Column(name = "pair_code", nullable = false, length = 32, updatable = false)
    private String pairCode;

    @Column(name = "assignment_code", nullable = false, length = 32, updatable = false)
    private String assignmentCode;

    @Column(name = "medium_code", nullable = false, length = 32, updatable = false)
    private String mediumCode;

    @Column(name = "comparable_fields", nullable = false, updatable = false)
    private String comparableFields;

    @Column(name = "comparable_count", nullable = false, updatable = false)
    private int comparableCount;

    @Column(name = "s_aa", nullable = false, updatable = false)
    private int sAA;

    @Column(name = "s_ab", nullable = false, updatable = false)
    private int sAB;

    @Column(name = "s_bb", nullable = false, updatable = false)
    private int sBB;

    @Column(name = "s_ba", nullable = false, updatable = false)
    private int sBA;

    @Column(name = "d", nullable = false, updatable = false)
    private int d;

    @Column(name = "computed_at", nullable = false, updatable = false)
    private Instant computedAt;

    protected StructuredScoreEntity() {
    }

    StructuredScoreEntity(Long studyId, String pairCode, String assignmentCode, String mediumCode,
                          List<String> comparableFields, int sAA, int sAB, int sBB, int sBA, Instant computedAt) {
        this.studyId = studyId;
        this.pairCode = pairCode;
        this.assignmentCode = assignmentCode;
        this.mediumCode = mediumCode;
        this.comparableFields = String.join(",", comparableFields);
        this.comparableCount = comparableFields.size();
        this.sAA = sAA;
        this.sAB = sAB;
        this.sBB = sBB;
        this.sBA = sBA;
        this.d = (sAA - sAB) + (sBB - sBA);
        this.computedAt = computedAt;
    }

    String pairCode() {
        return pairCode;
    }

    String assignmentCode() {
        return assignmentCode;
    }

    String mediumCode() {
        return mediumCode;
    }

    List<String> comparableFields() {
        return comparableFields.isEmpty() ? List.of() : Arrays.asList(comparableFields.split(","));
    }

    int comparableCount() {
        return comparableCount;
    }

    int sAA() {
        return sAA;
    }

    int sAB() {
        return sAB;
    }

    int sBB() {
        return sBB;
    }

    int sBA() {
        return sBA;
    }

    int d() {
        return d;
    }
}

/** The report of a P4 study (V20). Written once, at the unblinding. */
@Entity
@Table(name = "structured_report", schema = "research")
class StructuredReportEntity {

    @Id
    @Column(name = "study_id")
    private Long studyId;

    @Column(name = "pairs", nullable = false, updatable = false)
    private int pairs;

    @Column(name = "primary_p", nullable = false, length = 64, updatable = false)
    private String primaryP;

    @Column(name = "primary_method", nullable = false, updatable = false)
    private String primaryMethod;

    @Column(name = "sign_p", nullable = false, length = 64, updatable = false)
    private String signP;

    @Column(name = "permutation_p", nullable = false, length = 64, updatable = false)
    private String permutationP;

    @Column(name = "sitter_verified", nullable = false, updatable = false)
    private int sitterVerified;

    @Column(name = "sitter_total", nullable = false, updatable = false)
    private int sitterTotal;

    @Column(name = "reading_verified", nullable = false, updatable = false)
    private int readingVerified;

    @Column(name = "reading_total", nullable = false, updatable = false)
    private int readingTotal;

    @Column(name = "seed_hex", nullable = false, length = 64, updatable = false)
    private String seedHex;

    @Column(name = "document", nullable = false, updatable = false)
    private String document;

    @Column(name = "document_sha256", nullable = false, length = 64, updatable = false)
    private String documentSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StructuredReportEntity() {
    }

    StructuredReportEntity(Long studyId, int pairs, String primaryP, String primaryMethod, String signP,
                           String permutationP, int sitterVerified, int sitterTotal, int readingVerified,
                           int readingTotal, String seedHex, String document, String documentSha256,
                           Instant createdAt) {
        this.studyId = studyId;
        this.pairs = pairs;
        this.primaryP = primaryP;
        this.primaryMethod = primaryMethod;
        this.signP = signP;
        this.permutationP = permutationP;
        this.sitterVerified = sitterVerified;
        this.sitterTotal = sitterTotal;
        this.readingVerified = readingVerified;
        this.readingTotal = readingTotal;
        this.seedHex = seedHex;
        this.document = document;
        this.documentSha256 = documentSha256;
        this.createdAt = createdAt;
    }

    int pairs() {
        return pairs;
    }

    String primaryP() {
        return primaryP;
    }

    String primaryMethod() {
        return primaryMethod;
    }

    String signP() {
        return signP;
    }

    String permutationP() {
        return permutationP;
    }

    int sitterVerified() {
        return sitterVerified;
    }

    int sitterTotal() {
        return sitterTotal;
    }

    int readingVerified() {
        return readingVerified;
    }

    int readingTotal() {
        return readingTotal;
    }

    String seedHex() {
        return seedHex;
    }

    String document() {
        return document;
    }

    String documentSha256() {
        return documentSha256;
    }

    Instant createdAt() {
        return createdAt;
    }
}

interface StructuredScoreRepository extends JpaRepository<StructuredScoreEntity, Long> {

    List<StructuredScoreEntity> findByStudyIdOrderByPairCodeAsc(Long studyId);

    Optional<StructuredScoreEntity> findByPairCode(String pairCode);

    List<StructuredScoreEntity> findByAssignmentCodeIn(java.util.Collection<String> assignmentCodes);
}

interface StructuredReportRepository extends JpaRepository<StructuredReportEntity, Long> {
}
