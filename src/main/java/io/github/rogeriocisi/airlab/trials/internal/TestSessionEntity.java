package io.github.rogeriocisi.airlab.trials.internal;

import io.github.rogeriocisi.airlab.trials.SessionState;
import io.github.rogeriocisi.airlab.trials.TrialProtocol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A self-service session. The seed is escrowed encrypted at the start and written in clear
 * only when the session closes, the same scheme the draw of the paired protocol uses (P5).
 */
@Entity
@Table(name = "test_session", schema = "research")
class TestSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 48)
    private TrialProtocol protocol;

    @Column(name = "declared_trials", nullable = false)
    private int declaredTrials;

    @Column(name = "alternatives", nullable = false)
    private int alternatives;

    @Column(name = "seed_commitment", nullable = false, length = 64)
    private String seedCommitment;

    @Column(name = "seed_escrow", nullable = false)
    private String seedEscrow;

    @Column(name = "participant_nonce", length = 64)
    private String participantNonce;

    @Column(name = "server_seed_hex", length = 64)
    private String serverSeedHex;

    @Column(name = "seed_hex", length = 64)
    private String seedHex;

    @Column(name = "seed_revealed_at")
    private Instant seedRevealedAt;

    @Column(name = "target_pool_version", nullable = false, length = 16)
    private String targetPoolVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private SessionState state;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected TestSessionEntity() {
    }

    TestSessionEntity(String code, TrialProtocol protocol, int declaredTrials, String seedCommitment,
                      String seedEscrow, String targetPoolVersion, Instant startedAt) {
        this.code = code;
        this.protocol = protocol;
        this.declaredTrials = declaredTrials;
        this.alternatives = protocol.alternatives();
        this.seedCommitment = seedCommitment;
        this.seedEscrow = seedEscrow;
        this.targetPoolVersion = targetPoolVersion;
        this.state = SessionState.ACTIVE;
        this.startedAt = startedAt;
    }

    /**
     * Records the participant's half. Accepted once, before any trial exists, and only after
     * the commitment to the server's half is already published.
     */
    void contributeNonce(String nonceHex) {
        if (this.participantNonce != null) {
            throw new IllegalStateException(
                    "a sessao " + code + " ja recebeu a parte do participante");
        }
        this.participantNonce = nonceHex;
    }

    void close(SessionState finalState, String plainServerSeedHex, String plainSeedHex,
               Instant at) {
        if (this.state != SessionState.ACTIVE) {
            throw new IllegalStateException("session " + code + " is already closed");
        }
        this.state = finalState;
        this.serverSeedHex = plainServerSeedHex;
        this.seedHex = plainSeedHex;
        this.seedRevealedAt = at;
        this.closedAt = at;
    }

    String code() {
        return code;
    }

    TrialProtocol protocol() {
        return protocol;
    }

    int declaredTrials() {
        return declaredTrials;
    }

    int alternatives() {
        return alternatives;
    }

    String seedCommitment() {
        return seedCommitment;
    }

    String seedEscrow() {
        return seedEscrow;
    }

    String participantNonce() {
        return participantNonce;
    }

    String serverSeedHex() {
        return serverSeedHex;
    }

    String seedHex() {
        return seedHex;
    }

    String targetPoolVersion() {
        return targetPoolVersion;
    }

    SessionState state() {
        return state;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant closedAt() {
        return closedAt;
    }
}
