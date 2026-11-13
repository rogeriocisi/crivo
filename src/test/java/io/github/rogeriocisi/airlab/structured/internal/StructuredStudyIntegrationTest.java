package io.github.rogeriocisi.airlab.structured.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.allocation.AllocationEngine;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.structured.AssignmentState;
import io.github.rogeriocisi.airlab.structured.MediumAssignment;
import io.github.rogeriocisi.airlab.structured.MediumReadingService;
import io.github.rogeriocisi.airlab.structured.P4Fixture;
import io.github.rogeriocisi.airlab.structured.ReadingCommitment;
import io.github.rogeriocisi.airlab.structured.StructuredProgress;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyView;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * P4, phase C (SPEC E13) against the real PostgreSQL: the study, the pairing, the medium draw,
 * the release of the names, the locked readings, the deadline and the withdrawal — each P4 matrix
 * row about the medium as a test that tries to break it.
 *
 * <p>In the {@code internal} package because recomputing a reading's commitment needs its salt,
 * which only this module can decrypt — as the unblinding will.
 */
@SpringBootTest
@ActiveProfiles("test")
class StructuredStudyIntegrationTest {

    @Autowired
    StructuredStudyService studies;

    @Autowired
    MediumReadingService mediumReadings;

    @Autowired
    ConsultationService consultations;

    @Autowired
    AccountService accounts;

    @Autowired
    StructuredReadingRepository readings;

    @Autowired
    ReadingAssignmentRepository assignments;

    @Autowired
    FieldCipher cipher;

    @Autowired
    AdminNotices notices;

    @Autowired
    JdbcTemplate jdbc;

    P4Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new P4Fixture(accounts, consultations, studies, jdbc);
    }

    private ConsultationState stateOf(String requestCode) {
        return consultations.requestsByCode(List.of(requestCode)).get(0).state();
    }

    private List<String> trail(String subject) {
        return jdbc.queryForList("SELECT event_type || ' ' || actor_role || ' ' || coalesce(actor_code, '-') || ' ' "
                + "|| payload FROM audit.event WHERE subject_ref = ? ORDER BY seq", String.class, subject);
    }

    @Test
    @DisplayName("o estudo congela com o questionario no anexo e o compromisso da semente; um estudo por vez")
    void the_study_freezes_with_the_annex_and_the_seed_commitment() {
        StructuredStudyView study = fixture.ensureStudy();

        assertThat(study.frozenAt()).isNotNull();
        assertThat(study.seedCommitment()).matches("[0-9a-f]{64}");
        assertThat(study.document())
                .contains("Anexo do protocolo")
                .contains("PAIRED_FIELD_MATCHES")
                .contains("\"version\":\"" + study.questionnaireVersion() + "\"")
                .contains(study.seedCommitment())
                .contains("p4-medium:");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit.event WHERE event_type = 'ALLOCATION_SEED_COMMITTED' "
                + "AND subject_ref = ?", Long.class, "study:" + study.studyId())).isEqualTo(1L);

        assertThatThrownBy(() -> studies.open(io.github.rogeriocisi.airlab.structured.StructuredStudyTerms.defaults(),
                "pesquisador")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE research.structured_study SET target_pairs = 1 WHERE study_id = ?",
                study.studyId())).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("pedidos do mesmo estrato formam par e vao a um medium sorteado, que recebe so os dois nomes")
    void a_pair_goes_to_one_drawn_medium_who_alone_gets_the_names() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "18-39", "1-5-anos"), "Raimundo", "Joaquim");

        assertThat(stateOf(pair.a().code())).isEqualTo(ConsultationState.SENT_TO_MEDIUM);
        assertThat(stateOf(pair.b().code())).isEqualTo(ConsultationState.SENT_TO_MEDIUM);
        assertThat(accounts.activeMediumCodes()).contains(pair.medium());

        // P4 matrix: the first name goes to the drawn medium, and only for the requests received.
        assertThat(consultations.releasedFirstNames(pair.medium(), pair.assignment()))
                .containsOnly(Map.entry(pair.a().code(), "Raimundo"), Map.entry(pair.b().code(), "Joaquim"));
        String other = fixture.account(AccountProfile.MEDIUM);
        assertThat(consultations.releasedFirstNames(other, pair.assignment())).isEmpty();
        assertThat(mediumReadings.assignment(other, pair.assignment())).isEmpty();
        MediumAssignment seen = mediumReadings.assignment(pair.medium(), pair.assignment()).orElseThrow();
        assertThat(seen.slots()).extracting(MediumAssignment.Slot::firstName)
                .containsExactlyInAnyOrder("Raimundo", "Joaquim");

        // The trail links neither the send nor the release to the medium.
        List<String> sent = trail("assignment:" + pair.assignment());
        assertThat(sent).anyMatch(line -> line.startsWith("READING_REQUEST_SENT"))
                .anyMatch(line -> line.startsWith("FIRST_NAME_RELEASED"))
                .allMatch(line -> !line.contains(pair.medium()) && !line.contains("Raimundo"));
        assertThat(trail("pair:" + pair.code()))
                .allMatch(line -> !line.contains(pair.medium()) && !line.contains("masculino") && !line.contains("18-39"));

        // A second cycle sends nothing new for this pair.
        studies.runCycle(Instant.now());
        assertThat(assignments.findByPairCodeOrderByAttemptAsc(pair.code())).hasSize(1);
    }

    @Test
    @DisplayName("com a semente, o medium e a ordem dos nomes se refazem a partir do que foi registrado")
    void the_draw_is_reproducible_from_the_recorded_inputs() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "18-39", "5-15-anos"), "Benedito", "Sebastiao");
        StructuredStudyView study = fixture.ensureStudy();

        // As an auditor will after the unblinding: the escrowed seed, the recorded pool and order.
        String seed = cipher.decrypt(jdbc.queryForObject(
                "SELECT seed_escrow FROM research.allocation_set WHERE study_id = ?", String.class, study.studyId()));
        Map<String, Object> row = jdbc.queryForMap("SELECT attempt, medium_code, pool, first_request, second_request "
                + "FROM research.reading_assignment WHERE code = ?", pair.assignment());
        List<String> pool = List.of(((String) row.get("pool")).replaceAll("[\\[\\]\"]", "").split(","));
        int attempt = (Integer) row.get("attempt");

        assertThat(AllocationEngine.structuredMedium(seed, pair.code(), attempt, pool)).isEqualTo(row.get("medium_code"));
        assertThat(AllocationEngine.structuredNameOrder(seed, pair.code(), attempt, List.of(pair.b().code(), pair.a().code())))
                .containsExactly((String) row.get("first_request"), (String) row.get("second_request"));
    }

    @Test
    @DisplayName("a leitura trava com compromisso salgado, sem o codigo do medium na trilha, e o banco a protege")
    void readings_lock_with_a_salted_commitment_and_no_medium_on_the_trail() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "60-79", "menos-1-ano"), "Severino", "Expedito");
        String other = fixture.account(AccountProfile.MEDIUM);
        Map<String, String> answers = P4Fixture.mediumAnswers(1);

        assertThatThrownBy(() -> mediumReadings.lock(other, pair.assignment(), 1, answers))
                .isInstanceOf(IllegalStateException.class);
        mediumReadings.lock(pair.medium(), pair.assignment(), 1, answers);
        assertThatThrownBy(() -> mediumReadings.lock(pair.medium(), pair.assignment(), 1, answers))
                .isInstanceOf(IllegalStateException.class);
        assertThat(stateOf(pair.a().code())).isEqualTo(ConsultationState.SENT_TO_MEDIUM);

        StructuredReadingEntity reading = readings.findByAssignmentCodeOrderBySlotAsc(pair.assignment()).get(0);
        String salt = cipher.decrypt(reading.saltEnc());
        Map<String, String> stored = DefaultMediumReadingService.decodeAnswers(cipher.decrypt(reading.answersEnc()));
        assertThat(stored).isEqualTo(new java.util.TreeMap<>(answers));
        assertThat(ReadingCommitment.of(Questionnaires.current().version(), reading.code(), reading.requestCode(),
                stored, salt)).isEqualTo(reading.commitment());

        List<String> locked = trail("reading:" + reading.code());
        assertThat(locked).hasSize(1);
        assertThat(locked.get(0)).startsWith("STRUCTURED_READING_LOCKED MEDIUM - ")
                .contains(reading.commitment()).doesNotContain(pair.medium()).doesNotContain(salt);

        assertThatThrownBy(() -> jdbc.update("UPDATE research.structured_reading SET commitment = ? WHERE code = ?",
                "0".repeat(64), reading.code())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM research.structured_reading WHERE code = ?", reading.code()))
                .isInstanceOf(Exception.class);

        mediumReadings.lock(pair.medium(), pair.assignment(), 2, P4Fixture.mediumAnswers(2));
        assertThat(assignments.findByCode(pair.assignment()).orElseThrow().state()).isEqualTo(AssignmentState.COMPLETED);
        assertThat(stateOf(pair.a().code())).isEqualTo(ConsultationState.READING_RECEIVED);
        assertThat(stateOf(pair.b().code())).isEqualTo(ConsultationState.READING_RECEIVED);
        assertThat(notices.recent(50)).anyMatch(notice -> notice.kind() == NoticeKind.READING_RECEIVED
                && notice.subjectCode().equals(pair.code()));
        assertThat(mediumReadings.assignment(pair.medium(), pair.assignment()).orElseThrow().lockedSlots()).isEqualTo(2);
    }

    @Test
    @DisplayName("prazo vencido: o pedido vai a outro medium, e o anterior deixa de ver os nomes")
    void an_expired_request_goes_to_another_medium() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "40-59", "1-5-anos"), "Hermes", "Virgilio");
        fixture.account(AccountProfile.MEDIUM);
        mediumReadings.lock(pair.medium(), pair.assignment(), 1, P4Fixture.mediumAnswers(0));

        studies.runCycle(Instant.now().plus(Duration.ofDays(15)));

        List<ReadingAssignmentEntity> history = assignments.findByPairCodeOrderByAttemptAsc(pair.code());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).state()).isEqualTo(AssignmentState.EXPIRED);
        assertThat(history.get(1).state()).isEqualTo(AssignmentState.OPEN);
        assertThat(history.get(1).mediumCode()).isNotEqualTo(pair.medium());

        assertThat(consultations.releasedFirstNames(pair.medium(), pair.assignment())).isEmpty();
        assertThat(mediumReadings.assignment(pair.medium(), pair.assignment())).isEmpty();
        assertThat(consultations.releasedFirstNames(history.get(1).mediumCode(), history.get(1).code())).hasSize(2);
        // The reading locked before the deadline stays on record, out of the analysis.
        assertThat(readings.findByAssignmentCodeOrderBySlotAsc(pair.assignment())).hasSize(1);
        assertThat(notices.recent(100)).anyMatch(notice -> notice.kind() == NoticeKind.READING_OVERDUE
                && notice.subjectCode().equals(pair.code()));
        assertThatThrownBy(() -> mediumReadings.lock(pair.medium(), pair.assignment(), 2, P4Fixture.mediumAnswers(0)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("retirada do consulente: o par se desfaz, o envio e cancelado e o outro pedido volta a esperar")
    void a_withdrawal_dissolves_the_pair() {
        P4Fixture.Stratum stratum = new P4Fixture.Stratum("masculino", "60-79", "1-5-anos");
        P4Fixture.Pair pair = fixture.pairOf(stratum, "Anselmo", "Teodoro");

        accounts.close(pair.a().sitter());
        // The medium stops seeing the withdrawn name at once, before any cycle.
        assertThat(consultations.releasedFirstNames(pair.medium(), pair.assignment()))
                .doesNotContainKey(pair.a().code());
        assertThat(mediumReadings.assignment(pair.medium(), pair.assignment())).isEmpty();

        studies.runCycle(Instant.now());

        assertThat(jdbc.queryForObject("SELECT dissolution_reason FROM research.structured_pair WHERE code = ?",
                String.class, pair.code())).isEqualTo("WITHDRAWAL");
        assertThat(assignments.findByCode(pair.assignment()).orElseThrow().state()).isEqualTo(AssignmentState.CANCELLED);
        assertThat(stateOf(pair.b().code())).isEqualTo(ConsultationState.LOCKED);
        assertThat(consultations.releasedFirstNames(pair.medium(), pair.assignment())).isEmpty();
        assertThat(fixture.waiting(stratum)).isEqualTo(1);
    }

    @Test
    @DisplayName("o banco recusa par sobre pedido inexistente e qualquer reescrita de par, envio ou liberacao")
    void the_database_refuses_pairs_without_locked_requests_and_rewrites() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "80-mais", "5-15-anos"), "Abilio", "Onofre");
        long study = fixture.ensureStudy().studyId();

        assertThatThrownBy(() -> jdbc.update("INSERT INTO research.structured_pair (code, study_id, request_a, request_b, "
                + "formed_at) VALUES ('PAR-FALSO', ?, 'PED-NAO-EXISTE', ?, now())", study, pair.a().code()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE research.reading_assignment SET medium_code = 'MED-OUTRO' WHERE code = ?",
                pair.assignment())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE research.reading_assignment SET state = 'OPEN', closed_at = NULL, "
                + "due_at = due_at + interval '1 day' WHERE code = ?", pair.assignment())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM research.structured_pair WHERE code = ?", pair.code()))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE identity.first_name_release SET medium_code = 'MED-OUTRO' "
                + "WHERE assignment_code = ?", pair.assignment())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM identity.first_name_release WHERE assignment_code = ?",
                pair.assignment())).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("o progresso e contagem e o fluxo do pedido so anda pelos passos previstos")
    void progress_is_counts_and_the_flow_has_only_its_steps() {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("masculino", "ate-17", "1-5-anos"), "Caetano", "Romeu");

        StructuredProgress progress = studies.progress();
        assertThat(progress.targetPairs()).isEqualTo(fixture.ensureStudy().targetPairs());
        assertThat(progress.activePairs()).isPositive();
        assertThat(progress.openAssignments()).isPositive();

        ConsultationRef request = consultations.requestsByCode(List.of(pair.a().code())).get(0);
        assertThatThrownBy(() -> consultations.advance(request.code(), ConsultationState.SENT_TO_MEDIUM,
                ConsultationState.UNBLINDED)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> consultations.advance(request.code(), ConsultationState.LOCKED,
                ConsultationState.PAIRED)).isInstanceOf(IllegalStateException.class);
    }
}
