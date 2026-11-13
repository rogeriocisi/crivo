package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.ConsultationCommitment;
import io.github.rogeriocisi.airlab.participant.ConsultationCounts;
import io.github.rogeriocisi.airlab.participant.ConsultationDraft;
import io.github.rogeriocisi.airlab.participant.ConsultationLocked;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.participant.ConsultationStatus;
import io.github.rogeriocisi.airlab.participant.OwnConsultation;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Seeds;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P4 consultation requests (SPEC E13, phase B).
 *
 * <p>A request is stored once, already locked: the draft lives in the browser session, so there
 * is no window in which a stored request could change before its commitment is published.
 */
@Service
class DefaultConsultationService implements ConsultationService {

    /** Letters, with the spaces, apostrophes, dots and hyphens of real first names. No digits. */
    private static final Pattern FIRST_NAME = Pattern.compile("^\\p{L}[\\p{L}\\p{M}' .-]*$");

    private final ConsultationRequestRepository requests;
    private final ConsultationDetailRepository details;
    private final AccountRepository accounts;
    private final FieldCipher cipher;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    DefaultConsultationService(ConsultationRequestRepository requests,
                               ConsultationDetailRepository details,
                               AccountRepository accounts,
                               FieldCipher cipher,
                               AuditTrail auditTrail,
                               ApplicationEventPublisher events,
                               Clock clock) {
        this.requests = requests;
        this.details = details;
        this.accounts = accounts;
        this.cipher = cipher;
        this.auditTrail = auditTrail;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void check(ConsultationDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Preencha o pedido.");
        }
        String firstName = draft.firstName() == null ? "" : draft.firstName().strip();
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("Informe o primeiro nome da pessoa que faleceu.");
        }
        if (firstName.length() > MAXIMUM_FIRST_NAME_LENGTH) {
            throw new IllegalArgumentException("O primeiro nome pode ter no máximo "
                    + MAXIMUM_FIRST_NAME_LENGTH + " caracteres.");
        }
        if (!FIRST_NAME.matcher(firstName).matches()) {
            throw new IllegalArgumentException("Use só letras no primeiro nome, sem números nem símbolos.");
        }
        Questionnaires.current().checkSitterAnswers(draft.answers());
    }

    @Override
    @Transactional
    public ConsultationStatus submit(String sitterCode, ConsultationDraft draft) {
        check(draft);
        AccountEntity account = accounts.findByCode(sitterCode)
                .filter(candidate -> !candidate.closed() && candidate.confirmed())
                .filter(candidate -> candidate.profile() == AccountProfile.SITTER)
                .orElseThrow(() -> new IllegalStateException("only an active, confirmed consulente can file a request"));
        if (requests.findBySitterCodeAndStateNot(account.code(), ConsultationState.WITHDRAWN).isPresent()) {
            throw new IllegalStateException("this consulente already has an active request");
        }

        Questionnaire questionnaire = Questionnaires.current();
        String firstName = draft.firstName().strip();
        Map<String, String> answers = new TreeMap<>(draft.answers());
        String salt = Seeds.newSeedHex();
        String commitment = ConsultationCommitment.of(questionnaire.version(), firstName, answers, salt);
        // PostgreSQL keeps microseconds: truncate here, so the receipt shows what is stored.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        String code = Codes.of("PED");

        details.save(new ConsultationDetailEntity(code, cipher.encrypt(firstName),
                cipher.encrypt(encodeAnswers(answers)), cipher.encrypt(salt), now));
        ConsultationRequestEntity request = requests.save(new ConsultationRequestEntity(code, account.code(),
                questionnaire.version(),
                answers.get(Questionnaires.GENDER),
                answers.get(Questionnaires.AGE_BAND),
                answers.get(Questionnaires.DEATH_PERIOD),
                commitment, now));

        // The public trail gets the commitment and nothing it commits to (P6, P7).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request", code);
        payload.put("sitter", account.code());
        payload.put("questionnaire", questionnaire.version());
        payload.put("commitment", commitment);
        auditTrail.append(AuditEventType.CONSULTATION_LOCKED, ActorRole.SITTER, account.code(),
                "request:" + code, payload);
        events.publishEvent(new ConsultationLocked(code));

        return statusOf(request);
    }

    /**
     * Withdraws the sitter's active request, if any: the encrypted detail is erased, the request
     * stays as withdrawn, and the trail records it. Called when the account is closed.
     */
    @Transactional
    void withdrawActive(String sitterCode) {
        requests.findBySitterCodeAndStateNot(sitterCode, ConsultationState.WITHDRAWN).ifPresent(request -> {
            Instant now = clock.instant();
            details.findByRequestCode(request.code())
                    .filter(detail -> !detail.erased())
                    .ifPresent(detail -> detail.erase(now));
            request.withdraw(now);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("request", request.code());
            payload.put("sitter", sitterCode);
            auditTrail.append(AuditEventType.CONSULTATION_WITHDRAWN, ActorRole.SITTER, sitterCode,
                    "request:" + request.code(), payload);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsultationStatus> status(String sitterCode) {
        return requests.findBySitterCodeAndStateNot(sitterCode, ConsultationState.WITHDRAWN)
                .map(DefaultConsultationService::statusOf);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnConsultation> own(String sitterCode) {
        return requests.findBySitterCodeAndStateNot(sitterCode, ConsultationState.WITHDRAWN)
                .flatMap(request -> details.findByRequestCode(request.code())
                        .filter(detail -> !detail.erased())
                        .map(detail -> new OwnConsultation(request.code(), request.questionnaireVersion(),
                                cipher.decrypt(detail.firstNameEnc()),
                                decodeAnswers(cipher.decrypt(detail.answersEnc())),
                                request.commitment(), request.lockedAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationCounts counts() {
        return new ConsultationCounts(
                requests.countByStateNot(ConsultationState.WITHDRAWN),
                requests.countByState(ConsultationState.WITHDRAWN));
    }

    private static ConsultationStatus statusOf(ConsultationRequestEntity request) {
        return new ConsultationStatus(request.code(), request.state(), request.questionnaireVersion(),
                request.commitment(), request.lockedAt());
    }

    /**
     * Answers as {@code key=option} lines, sorted by key. Keys and options are slugs, so the
     * format needs no escaping; it is the stored form, while the commitment uses the canonical
     * JSON.
     */
    static String encodeAnswers(Map<String, String> answers) {
        StringBuilder out = new StringBuilder();
        new TreeMap<>(answers).forEach((key, value) -> out.append(key).append('=').append(value).append('\n'));
        return out.toString();
    }

    static Map<String, String> decodeAnswers(String encoded) {
        Map<String, String> answers = new TreeMap<>();
        for (String line : encoded.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            int equals = line.indexOf('=');
            answers.put(line.substring(0, equals), line.substring(equals + 1));
        }
        return answers;
    }
}
