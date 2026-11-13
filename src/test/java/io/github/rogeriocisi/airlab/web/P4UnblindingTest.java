package io.github.rogeriocisi.airlab.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.Mailer;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.ConsultationDraft;
import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.structured.FeedbackService;
import io.github.rogeriocisi.airlab.structured.MediumReadingService;
import io.github.rogeriocisi.airlab.structured.P4Fixture;
import io.github.rogeriocisi.airlab.structured.StructuredReport;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyTerms;
import io.github.rogeriocisi.airlab.structured.StructuredStudyView;
import io.github.rogeriocisi.airlab.structured.TargetFeedback;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * P4, phase D (SPEC E13) from end to end: a study with N = 2 collects, unblinds itself, scores,
 * analyses, reports, and hands each person their feedback — and nothing of that exists a moment
 * before the last reading locks.
 *
 * <p>On a database of its own, recreated on every run: the shared test database keeps an open P4
 * study with a large N, which never completes, and only one P4 study may be open at a time.
 *
 * <p>The readings here are made to fit: each one repeats the answers of the sitter it was made
 * for, and the two sitters of a pair differ on every field. So every number is known in advance —
 * which is the point of a test, not a claim about any medium.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "airlab.local-database.database-name=airlab_test_desmascaramento",
        "airlab.local-database.recreate-database=true"})
class P4UnblindingTest {

    private static final String FIRST_FIELD = Questionnaires.current().scoredFields().get(0).key();
    private static final int SCORED = Questionnaires.current().scoredFields().size();

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountService accounts;

    @Autowired
    ConsultationService consultations;

    @Autowired
    StructuredStudyService studies;

    @Autowired
    MediumReadingService mediumReadings;

    @Autowired
    FeedbackService feedback;

    @Autowired
    AdminNotices notices;

    @Autowired
    Mailer mailer;

    @Autowired
    JdbcTemplate jdbc;

    P4Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new P4Fixture(accounts, consultations, studies, jdbc);
    }

    private record Lock(String medium, String assignment, int position, Map<String, String> answers) {
    }

    /** A study with its four requests paired and sent, every reading locked but the last one. */
    private record Scenario(StructuredStudyView study, P4Fixture.Request a1, P4Fixture.Request b1,
                            P4Fixture.Request a2, P4Fixture.Request b2, String medium, Lock last) {

        List<String> requests() {
            return List.of(a1.code(), b1.code(), a2.code(), b2.code());
        }
    }

    private P4Fixture.Request file(P4Fixture.Stratum stratum, String firstName, int variant, boolean unknownFirst) {
        String sitter = fixture.account(AccountProfile.SITTER);
        Map<String, String> answers = new HashMap<>(P4Fixture.answers(stratum, variant));
        if (unknownFirst) {
            answers.put(FIRST_FIELD, Questionnaire.UNKNOWN);
        }
        String code = consultations.submit(sitter, new ConsultationDraft(firstName, answers)).code();
        return new P4Fixture.Request(code, sitter, firstName);
    }

    private Scenario scenario(String nameA1, String nameB1, String nameA2, String nameB2) {
        // Each scenario leaves its study reported, which is what lets the next one open.
        assertThat(studies.current()).isEmpty();
        fixture.account(AccountProfile.MEDIUM);
        StructuredStudyView study = studies.open(new StructuredStudyTerms("Estudo de desmascaramento", 2, 14, 0.5,
                "As leituras coincidem mais com o alvo.", "Qualquer medium confirmado."), "pesquisador");

        P4Fixture.Stratum first = new P4Fixture.Stratum("feminino", "60-79", "1-5-anos");
        P4Fixture.Stratum second = new P4Fixture.Stratum("masculino", "40-59", "mais-15-anos");
        P4Fixture.Request a1 = file(first, nameA1, 1, true);
        P4Fixture.Request b1 = file(first, nameB1, 2, false);
        P4Fixture.Request a2 = file(second, nameA2, 1, false);
        P4Fixture.Request b2 = file(second, nameB2, 2, false);
        Map<String, Integer> variant = Map.of(a1.code(), 1, b1.code(), 2, a2.code(), 1, b2.code(), 2);
        studies.runCycle(Instant.now());

        List<Lock> locks = new ArrayList<>();
        for (Map<String, Object> sent : jdbc.queryForList("SELECT code, medium_code, first_request, second_request "
                + "FROM research.reading_assignment WHERE state = 'OPEN' AND pair_code IN "
                + "(SELECT code FROM research.structured_pair WHERE study_id = ?) ORDER BY code", study.studyId())) {
            for (int position = 1; position <= 2; position++) {
                String request = (String) sent.get(position == 1 ? "first_request" : "second_request");
                locks.add(new Lock((String) sent.get("medium_code"), (String) sent.get("code"), position,
                        P4Fixture.mediumAnswers(variant.get(request))));
            }
        }
        assertThat(locks).hasSize(4);
        for (Lock lock : locks.subList(0, 3)) {
            mediumReadings.lock(lock.medium(), lock.assignment(), lock.position(), lock.answers());
        }
        Lock last = locks.get(3);
        return new Scenario(study, a1, b1, a2, b2, last.medium(), last);
    }

    private void lockLast(Scenario scenario) {
        Lock last = scenario.last();
        mediumReadings.lock(last.medium(), last.assignment(), last.position(), last.answers());
    }

    @Test
    @DisplayName("no N o estudo se desmascara sozinho, confere, analisa e relata; antes, nenhuma nota existe")
    void the_study_unblinds_itself_at_n_and_reports() {
        Scenario scenario = scenario("Aurora", "Benedita", "Caetano", "Damiao");
        String code = scenario.study().studyCode();
        long studyId = scenario.study().studyId();

        // Before the last reading: no report, no feedback, no score — not even by SQL.
        assertThat(studies.report(code)).isEmpty();
        assertThat(feedback.forRequest(scenario.a1().code())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM research.structured_score WHERE study_id = ?",
                Long.class, studyId)).isZero();
        Map<String, Object> anyPair = jdbc.queryForMap("SELECT p.code AS pair, a.code AS assignment FROM "
                + "research.structured_pair p JOIN research.reading_assignment a ON a.pair_code = p.code "
                + "WHERE p.study_id = ? LIMIT 1", studyId);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO research.structured_score (study_id, pair_code, "
                + "assignment_code, medium_code, comparable_fields, comparable_count, s_aa, s_ab, s_bb, s_ba, d, "
                + "computed_at) VALUES (?, ?, ?, 'MED-X', '', 0, 0, 0, 0, 0, 0, now())",
                studyId, anyPair.get("pair"), anyPair.get("assignment"))).isInstanceOf(Exception.class);
        assertThat(studies.runCycle(Instant.now()).unblindedStudies()).isZero();

        lockLast(scenario);
        assertThat(studies.runCycle(Instant.now()).unblindedStudies()).isEqualTo(1);

        assertThat(studies.current()).isEmpty();
        assertThat(studies.concluded()).extracting(StructuredStudyView::studyCode).contains(code);
        StructuredReport report = studies.report(code).orElseThrow();
        assertThat(report.pairs()).isEqualTo(2);
        // The pair with a "não sei" has one comparable field less.
        assertThat(report.pairResults()).extracting(StructuredReport.PairResult::d)
                .containsExactlyInAnyOrder(2 * (SCORED - 1), 2 * SCORED);
        assertThat(report.primaryPValue()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(report.signPValue()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(report.permutationPValue()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(report.integrityHolds()).isTrue();
        assertThat(report.sitterCommitments()).isEqualTo(4);
        assertThat(report.readingCommitments()).isEqualTo(4);
        assertThat(report.document()).contains(report.seedHex()).contains("Wilcoxon").contains(scenario.medium());
        assertThat(Hashes.sha256Hex(report.document())).isEqualTo(report.documentSha256());

        assertThat(jdbc.queryForObject("SELECT payload FROM audit.event WHERE event_type = 'REPORT_GENERATED' "
                + "AND subject_ref = ?", String.class, "study:" + studyId)).contains(report.documentSha256());
        assertThat(jdbc.queryForObject("SELECT actor_role FROM audit.event WHERE event_type = 'SEED_REVEALED' "
                + "AND subject_ref = ?", String.class, "study:" + studyId)).isEqualTo("SYSTEM");
        assertThat(jdbc.queryForObject("SELECT seed_hex FROM research.allocation_set WHERE study_id = ?",
                String.class, studyId)).isEqualTo(report.seedHex());
        assertThat(consultations.requestsByCode(scenario.requests())).extracting(ConsultationRef::state)
                .containsOnly(ConsultationState.UNBLINDED);
        assertThat(notices.recent(50)).anyMatch(notice -> notice.kind() == NoticeKind.STUDY_UNBLINDED
                && notice.subjectCode().equals(code));
        assertThat(notices.recent(50)).anyMatch(notice -> notice.kind() == NoticeKind.REPORT_READY
                && notice.subjectCode().equals(code));
        assertThat(mailer.outbox()).anyMatch(message -> message.subject().contains("devolutiva está disponível"));

        // Feedback, now that it exists: the sitter's own target, and counts only for the decoy.
        TargetFeedback own = feedback.forRequest(scenario.a1().code()).orElseThrow();
        assertThat(own.comparableFields()).hasSize(SCORED - 1).doesNotContain(FIRST_FIELD);
        assertThat(own.targetMatches()).isEqualTo(SCORED - 1);
        assertThat(own.decoyMatches()).isZero();
        assertThat(own.readingAnswers()).isEqualTo(P4Fixture.mediumAnswers(1));
        assertThat(feedback.forMedium(scenario.medium()))
                .anyMatch(reading -> reading.studyCode().equals(code) && reading.comparableFields() > 0);

        // Written once: the database refuses any change, and the next cycle does nothing.
        assertThatThrownBy(() -> jdbc.update("UPDATE research.structured_score SET d = 0 WHERE study_id = ?", studyId))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM research.structured_report WHERE study_id = ?", studyId))
                .isInstanceOf(Exception.class);
        assertThat(studies.runCycle(Instant.now()).unblindedStudies()).isZero();
    }

    @Test
    @DisplayName("a devolutiva aparece so depois do desmascaramento, para cada um a sua")
    void the_pages_show_the_feedback_only_after_the_unblinding() throws Exception {
        Scenario scenario = scenario("Eulalia", "Felisberta", "Gaspar", "Horacio");
        RequestPostProcessor a1 = user(scenario.a1().sitter()).roles("SITTER");
        RequestPostProcessor b1 = user(scenario.b1().sitter()).roles("SITTER");

        mvc.perform(get("/painel/consulente/devolutiva").with(a1)).andExpect(redirectedUrl("/painel/consulente"));

        lockLast(scenario);
        studies.runCycle(Instant.now());

        mvc.perform(get("/painel/consulente").with(a1))
                .andExpect(content().string(containsString("Ver a devolutiva")));
        mvc.perform(get("/painel/consulente/devolutiva").with(a1))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Eulalia")))
                .andExpect(content().string(containsString("Fora da comparação")))
                .andExpect(content().string(containsString((SCORED - 1) + " de " + (SCORED - 1))))
                .andExpect(content().string(containsString("0 de " + (SCORED - 1))))
                .andExpect(content().string(containsString("O resultado do estudo inteiro")))
                .andExpect(content().string(not(containsString("Felisberta"))))
                .andExpect(content().string(not(containsString(scenario.medium()))));
        // The other sitter of the pair sees the same field out of the comparison, and not the first's name.
        mvc.perform(get("/painel/consulente/devolutiva").with(b1))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fora da comparação")))
                .andExpect(content().string(not(containsString("Eulalia"))));

        mvc.perform(get("/painel/medium").with(user(scenario.medium()).roles("MEDIUM")))
                .andExpect(content().string(containsString("Notas das suas leituras")))
                .andExpect(content().string(containsString(scenario.study().studyCode())));

        RequestPostProcessor admin = user("pesquisador").roles("ADMIN", "RESEARCHER");
        mvc.perform(get("/painel/adm/estudo-p4").with(admin))
                .andExpect(content().string(containsString("Estudos concluídos")))
                .andExpect(content().string(containsString(scenario.study().studyCode())));
        mvc.perform(get("/painel/adm/estudo-p4/{code}", scenario.study().studyCode()).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Relatório")))
                .andExpect(content().string(containsString("Por médium")))
                .andExpect(content().string(containsString(studies.report(scenario.study().studyCode())
                        .orElseThrow().seedHex())));
    }
}
