package io.github.rogeriocisi.airlab.study.internal;

import io.github.rogeriocisi.airlab.study.StudyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "study", schema = "research")
class StudyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "protocol", nullable = false, length = 32)
    private String protocol;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StudyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StudyEntity() {
    }

    StudyEntity(String code, String title, String protocol, Instant createdAt) {
        this.code = code;
        this.title = title;
        this.protocol = protocol;
        this.status = StudyStatus.DRAFT;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    String code() {
        return code;
    }

    String title() {
        return title;
    }

    String protocol() {
        return protocol;
    }

    StudyStatus status() {
        return status;
    }

    Instant createdAt() {
        return createdAt;
    }

    void status(StudyStatus next) {
        if (next.ordinal() < this.status.ordinal()) {
            throw new IllegalStateException(
                    "a study never goes back: " + this.status + " -> " + next);
        }
        this.status = next;
    }
}
