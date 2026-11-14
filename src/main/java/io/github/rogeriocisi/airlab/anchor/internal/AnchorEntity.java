package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One request to the authority (migration V22). Never updated nor deleted: the database refuses
 * both, and refuses a row whose hash is not the hash of the event at that position.
 */
@Entity
@Table(name = "timestamp_anchor", schema = "audit")
class AnchorEntity {

    static final String GRANTED = "GRANTED";
    static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "anchored_seq", nullable = false)
    private long anchoredSeq;

    @Column(name = "anchored_hash", nullable = false, length = 64)
    private String anchoredHash;

    @Column(name = "authority_url", nullable = false, length = 255)
    private String authorityUrl;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "failure_reason", length = 32)
    private String failureReason;

    @Column(name = "attested_at")
    private Instant attestedAt;

    @Column(name = "serial_hex", length = 80)
    private String serialHex;

    @Column(name = "response_der")
    private byte[] responseDer;

    @Column(name = "response_sha256", length = 64)
    private String responseSha256;

    protected AnchorEntity() {
    }

    private AnchorEntity(String code, long anchoredSeq, String anchoredHash, String authorityUrl,
                         Instant requestedAt, String status) {
        this.code = code;
        this.anchoredSeq = anchoredSeq;
        this.anchoredHash = anchoredHash;
        this.authorityUrl = authorityUrl;
        this.requestedAt = requestedAt;
        this.status = status;
    }

    static AnchorEntity granted(String code, long anchoredSeq, String anchoredHash, String authorityUrl,
                                Instant requestedAt, Instant attestedAt, String serialHex,
                                byte[] responseDer, String responseSha256) {
        AnchorEntity entity = new AnchorEntity(code, anchoredSeq, anchoredHash, authorityUrl, requestedAt, GRANTED);
        entity.attestedAt = attestedAt;
        entity.serialHex = serialHex;
        entity.responseDer = responseDer.clone();
        entity.responseSha256 = responseSha256;
        return entity;
    }

    static AnchorEntity failed(String code, long anchoredSeq, String anchoredHash, String authorityUrl,
                               Instant requestedAt, FailureReason reason) {
        AnchorEntity entity = new AnchorEntity(code, anchoredSeq, anchoredHash, authorityUrl, requestedAt, FAILED);
        entity.failureReason = reason.name();
        return entity;
    }

    Long id() {
        return id;
    }

    String code() {
        return code;
    }

    long anchoredSeq() {
        return anchoredSeq;
    }

    String anchoredHash() {
        return anchoredHash;
    }

    Instant requestedAt() {
        return requestedAt;
    }

    boolean isGranted() {
        return GRANTED.equals(status);
    }

    Instant attestedAt() {
        return attestedAt;
    }

    String serialHex() {
        return serialHex;
    }

    byte[] responseDer() {
        return responseDer == null ? null : responseDer.clone();
    }

    String responseSha256() {
        return responseSha256;
    }
}
