package io.github.rogeriocisi.airlab.study.internal;

import io.github.rogeriocisi.airlab.study.ProtocolDeviation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only; the database refuses UPDATE and DELETE (migration V12). */
@Entity
@Table(name = "protocol_deviation", schema = "research")
class ProtocolDeviationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "preregistration_version", nullable = false)
    private Integer preregistrationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private ProtocolDeviation.Kind kind;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false, length = 32)
    private String recordedBy;

    protected ProtocolDeviationEntity() {
    }

    ProtocolDeviationEntity(Long studyId, int preregistrationVersion, ProtocolDeviation.Kind kind,
                            String description, Instant occurredAt, Instant recordedAt,
                            String recordedBy) {
        this.studyId = studyId;
        this.preregistrationVersion = preregistrationVersion;
        this.kind = kind;
        this.description = description;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
    }

    ProtocolDeviation toRecord() {
        return new ProtocolDeviation(studyId, preregistrationVersion, kind, description,
                occurredAt, recordedAt, recordedBy);
    }
}
