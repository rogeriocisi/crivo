package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.participant.AccountConfirmed;
import io.github.rogeriocisi.airlab.participant.AccountCounts;
import io.github.rogeriocisi.airlab.participant.AccountCredentials;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountRegistration;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountSignUp;
import io.github.rogeriocisi.airlab.participant.AccountView;
import io.github.rogeriocisi.airlab.participant.EmailConfirmationResult;
import io.github.rogeriocisi.airlab.support.BlindIndex;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P4 accounts (SPEC E13, phase A).
 *
 * <p>Every state change is in the audit trail with the pseudonymous code and the profile only:
 * the trail is public (P7), and a name or an e-mail there would be a leak no one could take back.
 */
@Service
class DefaultAccountService implements AccountService {

    private static final String EMAIL_DOMAIN = "account-email";

    /** Deliberately loose: the confirmation link is the real check that the address works. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** bcrypt only reads the first 72 bytes; a longer password would be silently truncated. */
    private static final int MAXIMUM_PASSWORD_BYTES = 72;

    private final AccountRepository accounts;
    private final EmailConfirmationRepository confirmations;
    private final DefaultConsultationService consultations;
    private final FieldCipher cipher;
    private final BlindIndex blindIndex;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    DefaultAccountService(AccountRepository accounts,
                          EmailConfirmationRepository confirmations,
                          DefaultConsultationService consultations,
                          FieldCipher cipher,
                          BlindIndex blindIndex,
                          PasswordEncoder passwordEncoder,
                          AuditTrail auditTrail,
                          ApplicationEventPublisher events,
                          Clock clock) {
        this.accounts = accounts;
        this.confirmations = confirmations;
        this.consultations = consultations;
        this.cipher = cipher;
        this.blindIndex = blindIndex;
        this.passwordEncoder = passwordEncoder;
        this.auditTrail = auditTrail;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountRegistration register(AccountSignUp signUp) {
        validate(signUp);
        String email = signUp.email().strip();
        String fingerprint = blindIndex.of(EMAIL_DOMAIN, email);

        Optional<AccountEntity> existing = accounts.findByEmailFingerprintAndClosedAtIsNull(fingerprint);
        if (existing.isPresent()) {
            AccountEntity account = existing.get();
            if (account.confirmed()) {
                return new AccountRegistration(account.code(), account.profile(), null, true);
            }
            String token = issueConfirmation(account.code());
            audit(AuditEventType.ACCOUNT_CONFIRMATION_REISSUED, account, Map.of());
            return new AccountRegistration(account.code(), account.profile(), token, true);
        }

        AccountProfile profile = signUp.profile();
        AccountEntity account = accounts.save(new AccountEntity(
                Codes.of(profile.codePrefix()),
                profile,
                cipher.encrypt(signUp.fullName().strip()),
                cipher.encrypt(email),
                fingerprint,
                passwordEncoder.encode(signUp.password()),
                signUp.termVersion(),
                signUp.sensitiveDataAccepted(),
                clock.instant()));
        String token = issueConfirmation(account.code());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("termVersion", signUp.termVersion());
        payload.put("sensitiveDataAccepted", signUp.sensitiveDataAccepted());
        audit(AuditEventType.ACCOUNT_CREATED, account, payload);
        return new AccountRegistration(account.code(), profile, token, false);
    }

    /** Field checks, with messages written for the person filling in the form. */
    private static void validate(AccountSignUp signUp) {
        if (signUp.profile() == null) {
            throw new IllegalArgumentException("Escolha um perfil: consulente ou médium.");
        }
        if (signUp.fullName() == null || signUp.fullName().isBlank()) {
            throw new IllegalArgumentException("Informe o seu nome.");
        }
        if (signUp.fullName().strip().length() > 200) {
            throw new IllegalArgumentException("O nome pode ter no máximo 200 caracteres.");
        }
        if (signUp.email() == null || signUp.email().strip().length() > 254
                || !EMAIL.matcher(signUp.email().strip()).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        String password = signUp.password() == null ? "" : signUp.password();
        if (password.codePointCount(0, password.length()) < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "A senha precisa ter pelo menos " + MINIMUM_PASSWORD_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PASSWORD_BYTES) {
            throw new IllegalArgumentException("A senha é longa demais: use até 72 caracteres simples.");
        }
        if (!CURRENT_TERM_VERSION.equals(signUp.termVersion())) {
            // E10, lock 6: consent is always on the current version of the term.
            throw new IllegalArgumentException(
                    "O termo mudou desde que você abriu a página. Leia a versão atual e aceite de novo.");
        }
        if (!signUp.sensitiveDataAccepted()) {
            throw new IllegalArgumentException(
                    "Para criar a conta é preciso aceitar o termo, inclusive o tratamento de dados sensíveis.");
        }
    }

    private String issueConfirmation(String accountCode) {
        String token = Codes.accessToken();
        Instant now = clock.instant();
        confirmations.save(new EmailConfirmationEntity(accountCode, Hashes.sha256Hex(token), now,
                now.plus(Duration.ofHours(CONFIRMATION_HOURS))));
        return token;
    }

    @Override
    @Transactional
    public EmailConfirmationResult confirmEmail(String token) {
        if (token == null || token.isBlank()) {
            return EmailConfirmationResult.UNKNOWN;
        }
        Optional<EmailConfirmationEntity> found = confirmations.findByTokenSha256(Hashes.sha256Hex(token.strip()));
        if (found.isEmpty()) {
            return EmailConfirmationResult.UNKNOWN;
        }
        EmailConfirmationEntity confirmation = found.get();
        if (confirmation.usedAt() != null) {
            return EmailConfirmationResult.ALREADY_USED;
        }
        Instant now = clock.instant();
        if (!now.isBefore(confirmation.expiresAt())) {
            return EmailConfirmationResult.EXPIRED;
        }
        AccountEntity account = accounts.findByCode(confirmation.accountCode())
                .filter(candidate -> !candidate.closed())
                .orElse(null);
        if (account == null) {
            return EmailConfirmationResult.UNKNOWN;
        }

        confirmation.use(now);
        boolean firstConfirmation = !account.confirmed();
        account.confirmEmail(now);
        if (firstConfirmation) {
            audit(AuditEventType.ACCOUNT_EMAIL_CONFIRMED, account, Map.of());
            events.publishEvent(new AccountConfirmed(account.code(), account.profile()));
        }
        return EmailConfirmationResult.CONFIRMED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountCredentials> credentials(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return accounts.findByEmailFingerprintAndClosedAtIsNull(blindIndex.of(EMAIL_DOMAIN, email.strip()))
                .map(account -> new AccountCredentials(account.code(), account.passwordHash(),
                        account.profile(), account.confirmed()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountView> account(String code) {
        return accounts.findByCode(code)
                .filter(account -> !account.closed())
                .map(account -> new AccountView(account.code(), account.profile(),
                        firstNameOf(cipher.decrypt(account.fullNameEnc())), account.termVersion(),
                        account.createdAt(), account.emailConfirmedAt()));
    }

    private static String firstNameOf(String fullName) {
        String stripped = fullName.strip();
        int space = stripped.indexOf(' ');
        return space < 0 ? stripped : stripped.substring(0, space);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountCounts counts() {
        return new AccountCounts(
                accounts.countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNotNull(AccountProfile.SITTER),
                accounts.countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNull(AccountProfile.SITTER),
                accounts.countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNotNull(AccountProfile.MEDIUM),
                accounts.countByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNull(AccountProfile.MEDIUM));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> activeMediumCodes() {
        return accounts.findByProfileAndClosedAtIsNullAndEmailConfirmedAtIsNotNullOrderByCodeAsc(AccountProfile.MEDIUM)
                .stream().map(AccountEntity::code).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> mailAddress(String code) {
        return accounts.findByCode(code)
                .filter(account -> !account.closed() && account.confirmed())
                .map(account -> cipher.decrypt(account.emailEnc()));
    }

    @Override
    @Transactional
    public void close(String code) {
        AccountEntity account = accounts.findByCode(code)
                .filter(candidate -> !candidate.closed())
                .orElseThrow(() -> new IllegalArgumentException("unknown or closed account"));
        // Withdrawal reaches the request too: its encrypted detail is erased with the account.
        consultations.withdrawActive(account.code());
        account.close(clock.instant());
        audit(AuditEventType.ACCOUNT_CLOSED, account, Map.of());
    }

    private void audit(AuditEventType type, AccountEntity account, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("account", account.code());
        payload.put("profile", account.profile().name());
        payload.putAll(extra);
        ActorRole actor = account.profile() == AccountProfile.SITTER ? ActorRole.SITTER : ActorRole.MEDIUM;
        auditTrail.append(type, actor, account.code(), "account:" + account.code(), payload);
    }
}
