package io.github.rogeriocisi.airlab.structured;

import io.github.rogeriocisi.airlab.TestNames;
import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountRegistration;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountSignUp;
import io.github.rogeriocisi.airlab.participant.ConsultationDraft;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Builds P4 situations for the tests (SPEC E13, phase C) on the shared, persistent test database.
 *
 * <p>The database outlives each run and is shared by every test class, so a test cannot assume it
 * is alone: there may be an open P4 study already, requests left waiting by an earlier run, and
 * mediums from other tests in the pool. So each test uses a stratum of its own, and
 * {@link #pairOf} first sweeps that stratum — pairing whatever was left there — before it files
 * the two requests it will pair.
 */
public final class P4Fixture {

    public static final String PASSWORD = "senha-de-teste-123";

    /** Gender, age band and period of death: the pairing key. */
    public record Stratum(String gender, String ageBand, String deathPeriod) {
    }

    public record Request(String code, String sitter, String firstName) {
    }

    /** A pair with its open request to a medium. */
    public record Pair(String code, Request a, Request b, String assignment, String medium) {

        public List<String> requests() {
            return List.of(a.code(), b.code());
        }
    }

    private final AccountService accounts;
    private final ConsultationService consultations;
    private final StructuredStudyService studies;
    private final JdbcTemplate jdbc;

    public P4Fixture(AccountService accounts, ConsultationService consultations, StructuredStudyService studies,
                     JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.consultations = consultations;
        this.studies = studies;
        this.jdbc = jdbc;
    }

    /** The open P4 study, created with a large N when there is none. */
    public StructuredStudyView ensureStudy() {
        return studies.current().orElseGet(() -> studies.open(new StructuredStudyTerms("Estudo P4 de teste",
                StructuredStudyTerms.MAXIMUM_TARGET_PAIRS, StructuredStudyTerms.DEFAULT_DEADLINE_DAYS, 0.5,
                "Hipotese de teste.", "Qualquer medium confirmado.", StructuredStudyTerms.MAXIMUM_TARGET_PAIRS),
                "pesquisador"));
    }

    /** A confirmed account; its e-mail is random and its password is {@link #PASSWORD}. */
    public String account(AccountProfile profile) {
        return account(profile, TestNames.unique("Pessoa"));
    }

    /** A confirmed account with a chosen full name — to make two accounts share one on purpose. */
    public String account(AccountProfile profile, String fullName) {
        AccountRegistration registration = accounts.register(new AccountSignUp(fullName,
                "p4-" + UUID.randomUUID() + "@exemplo.test", PASSWORD, profile,
                AccountService.CURRENT_TERM_VERSION, true));
        accounts.confirmEmail(registration.confirmationToken());
        return registration.code();
    }

    /**
     * Answers in a stratum: the pairing fields from it, and every scored field with the option at
     * {@code variant} (wrapping around), so two variants differ on every field.
     */
    public static Map<String, String> answers(Stratum stratum, int variant) {
        Map<String, String> answers = new HashMap<>();
        for (Questionnaire.Field field : Questionnaires.current().scoredFields()) {
            answers.put(field.key(), field.options().get(variant % field.options().size()).key());
        }
        answers.put(Questionnaires.GENDER, stratum.gender());
        answers.put(Questionnaires.AGE_BAND, stratum.ageBand());
        answers.put(Questionnaires.DEATH_PERIOD, stratum.deathPeriod());
        return answers;
    }

    /** A medium's answers for every scored field, with the option at {@code variant}. */
    public static Map<String, String> mediumAnswers(int variant) {
        Map<String, String> answers = new HashMap<>();
        for (Questionnaire.Field field : Questionnaires.current().scoredFields()) {
            answers.put(field.key(), field.options().get(variant % field.options().size()).key());
        }
        return answers;
    }

    public Request request(Stratum stratum, String firstName, int variant) {
        return request(stratum, firstName, variant, account(AccountProfile.SITTER));
    }

    /** A request filed by a given sitter account. */
    public Request request(Stratum stratum, String firstName, int variant, String sitter) {
        String code = consultations.submit(sitter, new ConsultationDraft(firstName, answers(stratum, variant))).code();
        return new Request(code, sitter, firstName);
    }

    /**
     * Two fresh requests of the stratum, paired together and sent to a drawn medium. Makes sure the
     * study exists and the pool has a medium; sweeps the stratum first.
     */
    public Pair pairOf(Stratum stratum, String firstNameA, String firstNameB) {
        ensureStudy();
        account(AccountProfile.MEDIUM);
        studies.runCycle(Instant.now());
        if (waiting(stratum) > 0) {
            request(stratum, "Sobrante", 0);
            studies.runCycle(Instant.now());
        }
        Request a = request(stratum, firstNameA, 1);
        Request b = request(stratum, firstNameB, 2);
        studies.runCycle(Instant.now());

        Map<String, Object> pair = jdbc.queryForMap("SELECT code, request_a, request_b FROM research.structured_pair "
                + "WHERE dissolved_at IS NULL AND (request_a = ? OR request_b = ?)", a.code(), a.code());
        if (!Set.of(pair.get("request_a"), pair.get("request_b")).equals(Set.of(a.code(), b.code()))) {
            throw new IllegalStateException("the fixture expected its two requests to be paired together");
        }
        Map<String, Object> assignment = jdbc.queryForMap("SELECT code, medium_code FROM research.reading_assignment "
                + "WHERE pair_code = ? AND state = 'OPEN'", pair.get("code"));
        return new Pair((String) pair.get("code"), a, b, (String) assignment.get("code"),
                (String) assignment.get("medium_code"));
    }

    /** Requests of the stratum waiting for a pair, in the questionnaire version in force. */
    public long waiting(Stratum stratum) {
        String key = stratum.gender() + "|" + stratum.ageBand() + "|" + stratum.deathPeriod();
        return consultations.requests(Set.of(ConsultationState.LOCKED)).stream()
                .filter(request -> request.questionnaireVersion().equals(Questionnaires.current().version()))
                .filter(request -> request.stratum().equals(key))
                .count();
    }
}
