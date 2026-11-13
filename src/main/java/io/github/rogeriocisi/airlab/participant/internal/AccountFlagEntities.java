package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.participant.FlagDecision;
import io.github.rogeriocisi.airlab.participant.LinkSignal;
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** A blind-indexed signal of a confirmed account (V21). Deleted on withdrawal, never changed. */
@Entity
@Table(name = "account_signal", schema = "identity")
class AccountSignalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 32, updatable = false)
    private String accountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24, updatable = false)
    private LinkSignal kind;

    @Column(name = "fingerprint", nullable = false, length = 64, updatable = false)
    private String fingerprint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountSignalEntity() {
    }

    AccountSignalEntity(String accountCode, LinkSignal kind, String fingerprint, Instant createdAt) {
        this.accountCode = accountCode;
        this.kind = kind;
        this.fingerprint = fingerprint;
        this.createdAt = createdAt;
    }

    String accountCode() {
        return accountCode;
    }
}

/** Two accounts sharing a signal, for human review (V21). Decided once, never deleted. */
@Entity
@Table(name = "account_flag", schema = "identity")
class AccountFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Column(name = "account_a", nullable = false, length = 32, updatable = false)
    private String accountA;

    @Column(name = "account_b", nullable = false, length = 32, updatable = false)
    private String accountB;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24, updatable = false)
    private LinkSignal kind;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 16)
    private FlagDecision decision;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewer", length = 64)
    private String reviewer;

    protected AccountFlagEntity() {
    }

    /** The two codes are stored in order, so a pair has one row per signal whoever came first. */
    AccountFlagEntity(String code, String one, String other, LinkSignal kind, Instant createdAt) {
        this.code = code;
        this.accountA = one.compareTo(other) < 0 ? one : other;
        this.accountB = one.compareTo(other) < 0 ? other : one;
        this.kind = kind;
        this.createdAt = createdAt;
    }

    void decide(FlagDecision decision, String reviewer, Instant at) {
        this.decision = decision;
        this.reviewer = reviewer;
        this.reviewedAt = at;
    }

    String code() {
        return code;
    }

    String accountA() {
        return accountA;
    }

    String accountB() {
        return accountB;
    }

    LinkSignal kind() {
        return kind;
    }

    Instant createdAt() {
        return createdAt;
    }

    FlagDecision decision() {
        return decision;
    }

    Instant reviewedAt() {
        return reviewedAt;
    }

    String other(String account) {
        return accountA.equals(account) ? accountB : accountA;
    }
}

interface AccountSignalRepository extends JpaRepository<AccountSignalEntity, Long> {

    List<AccountSignalEntity> findByKindAndFingerprint(LinkSignal kind, String fingerprint);

    boolean existsByAccountCodeAndKind(String accountCode, LinkSignal kind);

    void deleteByAccountCode(String accountCode);
}

interface AccountFlagRepository extends JpaRepository<AccountFlagEntity, Long> {

    Optional<AccountFlagEntity> findByCode(String code);

    boolean existsByAccountAAndAccountBAndKind(String accountA, String accountB, LinkSignal kind);

    long countByDecisionIsNull();

    List<AccountFlagEntity> findAllByOrderByCreatedAtDescIdDesc();

    @Query("select f from AccountFlagEntity f where (f.decision is null or f.decision <> io.github.rogeriocisi.airlab.participant.FlagDecision.UNRELATED) "
            + "and (f.accountA in :codes or f.accountB in :codes)")
    List<AccountFlagEntity> findKeepingApart(@Param("codes") Collection<String> codes);
}
