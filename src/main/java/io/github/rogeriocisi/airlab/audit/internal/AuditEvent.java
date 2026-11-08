package io.github.rogeriocisi.airlab.audit.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.Hashes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One link of the audit chain. Rows are never updated nor deleted: the database rejects
 * both with a trigger (migration V2), so a tampered row cannot be made to look valid.
 */
@Entity
@Table(name = "event", schema = "audit")
class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 32)
    private ActorRole actorRole;

    @Column(name = "actor_code", length = 64)
    private String actorCode;

    @Column(name = "subject_ref", length = 128)
    private String subjectRef;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    protected AuditEvent() {
    }

    AuditEvent(Instant occurredAt,
               AuditEventType eventType,
               ActorRole actorRole,
               String actorCode,
               String subjectRef,
               String canonicalPayload,
               String prevHash) {
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.actorRole = actorRole;
        this.actorCode = actorCode;
        this.subjectRef = subjectRef;
        this.payload = canonicalPayload;
        this.prevHash = prevHash;
        this.hash = computeHash();
    }

    /**
     * The hashed preimage, in canonical JSON. Deliberately excludes the database sequence:
     * an auditor must be able to recompute every hash from the exported trail alone, with
     * no knowledge of how rows were numbered.
     */
    String computeHash() {
        Map<String, Object> preimage = new LinkedHashMap<>();
        preimage.put("occurredAt", occurredAt.toString());
        preimage.put("eventType", eventType.name());
        preimage.put("actorRole", actorRole.name());
        preimage.put("actorCode", actorCode);
        preimage.put("subjectRef", subjectRef);
        preimage.put("payload", payload);
        preimage.put("prevHash", prevHash);
        return Hashes.sha256Hex(CanonicalJson.write(preimage));
    }

    Long seq() {
        return seq;
    }

    String hash() {
        return hash;
    }

    String prevHash() {
        return prevHash;
    }

    AuditEventType eventType() {
        return eventType;
    }

    String payload() {
        return payload;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
