package io.github.rogeriocisi.airlab.study.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One version of the preregistration. Once {@code frozenAt} is set the row is immutable,
 * enforced by a database trigger (migration V3) as well as by this class.
 */
@Entity
@Table(name = "preregistration_version", schema = "research")
class PreregistrationVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "document", nullable = false)
    private String document;

    @Column(name = "document_sha256", nullable = false, length = 64)
    private String documentSha256;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "timestamp_proof_ref", length = 256)
    private String timestampProofRef;

    @Column(name = "amendment_reason")
    private String amendmentReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PreregistrationVersionEntity() {
    }

    PreregistrationVersionEntity(Long studyId, int version, String document, String documentSha256,
                                 String amendmentReason, Instant createdAt) {
        this.studyId = studyId;
        this.version = version;
        this.document = document;
        this.documentSha256 = documentSha256;
        this.amendmentReason = amendmentReason;
        this.createdAt = createdAt;
    }

    void freeze(String frozenDocument, String frozenSha256, Instant at) {
        if (frozenAt != null) {
            throw new IllegalStateException("preregistration version " + version + " is already frozen");
        }
        this.document = frozenDocument;
        this.documentSha256 = frozenSha256;
        this.frozenAt = at;
    }

    void attachTimestampProof(String ref) {
        if (this.timestampProofRef != null) {
            throw new IllegalStateException("timestamp proof already attached to version " + version);
        }
        this.timestampProofRef = ref;
    }

    Long id() {
        return id;
    }

    Long studyId() {
        return studyId;
    }

    int version() {
        return version;
    }

    String document() {
        return document;
    }

    String documentSha256() {
        return documentSha256;
    }

    Instant frozenAt() {
        return frozenAt;
    }

    String timestampProofRef() {
        return timestampProofRef;
    }

    String amendmentReason() {
        return amendmentReason;
    }

    Instant createdAt() {
        return createdAt;
    }
}
