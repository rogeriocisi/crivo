package io.github.rogeriocisi.airlab.participant.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entities of the {@code identity} schema: identifiable and sensitive data (P6).
 *
 * <p>Fields ending in {@code Enc} hold ciphertext produced by the application
 * ({@code FieldCipher}); the database never sees the plain value.
 */
@Entity
@Table(name = "participant", schema = "identity")
class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "full_name_enc", nullable = false)
    private String fullNameEnc;

    @Column(name = "contact_enc")
    private String contactEnc;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected ParticipantEntity() {
    }

    ParticipantEntity(String code, String fullNameEnc, String contactEnc, Instant createdAt) {
        this.code = code;
        this.fullNameEnc = fullNameEnc;
        this.contactEnc = contactEnc;
        this.createdAt = createdAt;
    }

    /** Erases the identifiable data in place; the pseudonymous code survives (P6). */
    void erase(Instant at) {
        this.fullNameEnc = "";
        this.contactEnc = null;
        this.withdrawnAt = at;
    }

    String code() {
        return code;
    }

    String fullNameEnc() {
        return fullNameEnc;
    }

    Instant withdrawnAt() {
        return withdrawnAt;
    }
}

@Entity
@Table(name = "target_detail", schema = "identity")
class TargetDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_code", nullable = false, length = 32, unique = true)
    private String targetCode;

    @Column(name = "first_name_enc", nullable = false)
    private String firstNameEnc;

    @Column(name = "notes_enc")
    private String notesEnc;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TargetDetailEntity() {
    }

    TargetDetailEntity(String targetCode, String firstNameEnc, String notesEnc, Instant createdAt) {
        this.targetCode = targetCode;
        this.firstNameEnc = firstNameEnc;
        this.notesEnc = notesEnc;
        this.createdAt = createdAt;
    }

    String targetCode() {
        return targetCode;
    }

    String firstNameEnc() {
        return firstNameEnc;
    }
}

@Entity
@Table(name = "consent", schema = "identity")
class ConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_code", nullable = false, length = 32)
    private String participantCode;

    @Column(name = "consent_version", nullable = false, length = 32)
    private String consentVersion;

    @Column(name = "sensitive_data_ok", nullable = false)
    private boolean sensitiveDataOk;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ConsentEntity() {
    }

    ConsentEntity(String participantCode, String consentVersion, boolean sensitiveDataOk,
                  Instant grantedAt) {
        this.participantCode = participantCode;
        this.consentVersion = consentVersion;
        this.sensitiveDataOk = sensitiveDataOk;
        this.grantedAt = grantedAt;
    }

    void revoke(Instant at) {
        this.revokedAt = at;
    }

    String consentVersion() {
        return consentVersion;
    }
}
