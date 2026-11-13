package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.participant.AccountProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A P4 account, in the {@code identity} schema (V17). Fields ending in {@code Enc} hold
 * ciphertext produced by the application; the database never sees the plain value.
 *
 * <p>The profile has no setter, and the database refuses to change it anyway.
 */
@Entity
@Table(name = "account", schema = "identity")
class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile", nullable = false, length = 16, updatable = false)
    private AccountProfile profile;

    @Column(name = "full_name_enc", nullable = false)
    private String fullNameEnc;

    @Column(name = "email_enc")
    private String emailEnc;

    @Column(name = "email_fingerprint", length = 64)
    private String emailFingerprint;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "term_version", nullable = false, length = 32)
    private String termVersion;

    @Column(name = "sensitive_data_accepted", nullable = false)
    private boolean sensitiveDataAccepted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "email_confirmed_at")
    private Instant emailConfirmedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected AccountEntity() {
    }

    AccountEntity(String code, AccountProfile profile, String fullNameEnc, String emailEnc,
                  String emailFingerprint, String passwordHash, String termVersion,
                  boolean sensitiveDataAccepted, Instant createdAt) {
        this.code = code;
        this.profile = profile;
        this.fullNameEnc = fullNameEnc;
        this.emailEnc = emailEnc;
        this.emailFingerprint = emailFingerprint;
        this.passwordHash = passwordHash;
        this.termVersion = termVersion;
        this.sensitiveDataAccepted = sensitiveDataAccepted;
        this.createdAt = createdAt;
    }

    void confirmEmail(Instant at) {
        if (emailConfirmedAt == null) {
            this.emailConfirmedAt = at;
        }
    }

    /** Erases the identifiable data in place; the pseudonymous code survives (P6). */
    void close(Instant at) {
        this.fullNameEnc = "";
        this.emailEnc = null;
        this.emailFingerprint = null;
        this.passwordHash = null;
        this.closedAt = at;
    }

    String code() {
        return code;
    }

    AccountProfile profile() {
        return profile;
    }

    String fullNameEnc() {
        return fullNameEnc;
    }

    String emailEnc() {
        return emailEnc;
    }

    String passwordHash() {
        return passwordHash;
    }

    String termVersion() {
        return termVersion;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant emailConfirmedAt() {
        return emailConfirmedAt;
    }

    Instant closedAt() {
        return closedAt;
    }

    boolean confirmed() {
        return emailConfirmedAt != null;
    }

    boolean closed() {
        return closedAt != null;
    }
}

/** One-time e-mail confirmation link. Only the SHA-256 of the token is stored. */
@Entity
@Table(name = "email_confirmation", schema = "identity")
class EmailConfirmationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 32)
    private String accountCode;

    @Column(name = "token_sha256", nullable = false, length = 64, unique = true)
    private String tokenSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected EmailConfirmationEntity() {
    }

    EmailConfirmationEntity(String accountCode, String tokenSha256, Instant createdAt,
                            Instant expiresAt) {
        this.accountCode = accountCode;
        this.tokenSha256 = tokenSha256;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    void use(Instant at) {
        this.usedAt = at;
    }

    String accountCode() {
        return accountCode;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    Instant usedAt() {
        return usedAt;
    }
}

interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByCode(String code);

    Optional<AccountEntity> findByEmailFingerprintAndClosedAtIsNull(String emailFingerprint);

    long countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNotNull(AccountProfile profile);

    long countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNull(AccountProfile profile);

    java.util.List<AccountEntity> findByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNotNullOrderByCodeAsc(
            AccountProfile profile);
}

interface EmailConfirmationRepository extends JpaRepository<EmailConfirmationEntity, Long> {

    Optional<EmailConfirmationEntity> findByTokenSha256(String tokenSha256);
}
