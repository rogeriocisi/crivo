package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.TestNames;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountRegistration;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountSignUp;
import io.github.rogeriocisi.airlab.participant.ConsultationCommitment;
import io.github.rogeriocisi.airlab.participant.ConsultationDraft;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.participant.ConsultationStatus;
import io.github.rogeriocisi.airlab.participant.OwnConsultation;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * P4 consultation requests (SPEC E13, phase B) against the real PostgreSQL. The lock, the
 * salted commitment and the P4 visibility matrix rows that phase B touches, each as a test that
 * tries to break them.
 *
 * <p>In the {@code internal} package because recomputing the commitment needs the salt, which
 * only this module can decrypt — exactly as it will in the unblinding.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConsultationIntegrationTest {

    private static final String FIRST_NAME = "Sebastião";

    @Autowired
    AccountService accounts;

    @Autowired
    ConsultationService consultations;

    @Autowired
    ConsultationDetailRepository details;

    @Autowired
    FieldCipher cipher;

    @Autowired
    AdminNotices notices;

    @Autowired
    JdbcTemplate jdbc;

    private String confirmedAccount(AccountProfile profile) {
        AccountRegistration registration = accounts.register(new AccountSignUp(TestNames.unique("Pessoa"),
                "pedido-" + UUID.randomUUID() + "@exemplo.test", "senha-de-teste-123", profile,
                AccountService.CURRENT_TERM_VERSION, true));
        accounts.confirmEmail(registration.confirmationToken());
        return registration.code();
    }

    private static Map<String, String> answers() {
        Map<String, String> answers = new HashMap<>();
        for (Questionnaire.Field field : Questionnaires.current().fields()) {
            answers.put(field.key(), field.options().get(field.options().size() - 1).key());
        }
        answers.put("causa", Questionnaire.UNKNOWN);
        return answers;
    }

    private static ConsultationDraft draft() {
        return new ConsultationDraft(FIRST_NAME, answers());
    }

    @Test
    @DisplayName("o pedido nasce travado, com compromisso, e o consulente ve o proprio conteudo")
    void a_request_is_born_locked() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        assertThat(status.state()).isEqualTo(ConsultationState.LOCKED);
        assertThat(status.code()).startsWith("PED-");
        assertThat(status.commitment()).matches("[0-9a-f]{64}");
        assertThat(status.questionnaireVersion()).isEqualTo(Questionnaires.current().version());
        assertThat(consultations.status(sitter)).contains(status);

        OwnConsultation own = consultations.own(sitter).orElseThrow();
        assertThat(own.firstName()).isEqualTo(FIRST_NAME);
        assertThat(own.answers()).isEqualTo(answers());
    }

    @Test
    @DisplayName("o compromisso publicado se recalcula a partir do pedido revelado, e muda com qualquer resposta")
    void the_commitment_is_recomputable_and_binding() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        ConsultationDetailEntity detail = details.findByRequestCode(status.code()).orElseThrow();
        String salt = cipher.decrypt(detail.saltEnc());
        String version = status.questionnaireVersion();

        assertThat(ConsultationCommitment.of(version, FIRST_NAME, answers(), salt)).isEqualTo(status.commitment());

        Map<String, String> changed = answers();
        changed.put("fumava", "sim".equals(changed.get("fumava")) ? "nao" : "sim");
        assertThat(ConsultationCommitment.of(version, FIRST_NAME, changed, salt)).isNotEqualTo(status.commitment());
        assertThat(ConsultationCommitment.of(version, "Sebastiana", answers(), salt)).isNotEqualTo(status.commitment());
    }

    @Test
    @DisplayName("a trilha publica recebe o compromisso e nada do que ele compromete")
    void the_trail_gets_the_commitment_only() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        String payload = jdbc.queryForObject(
                "SELECT payload FROM audit.event WHERE subject_ref = ? AND event_type = 'CONSULTATION_LOCKED'",
                String.class, "request:" + status.code());
        assertThat(payload).contains(status.commitment()).doesNotContain(FIRST_NAME);
        for (String option : answers().values()) {
            assertThat(payload).describedAs("a resposta %s nao pode estar na trilha", option)
                    .doesNotContain("\"" + option + "\"");
        }
        assertThat(payload).doesNotContain("genero").doesNotContain("causa");
    }

    @Test
    @DisplayName("primeiro nome e respostas ficam cifrados no banco")
    void the_detail_is_encrypted() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        String stored = jdbc.queryForObject("SELECT first_name_enc || answers_enc || salt_enc "
                + "FROM identity.consultation_detail WHERE request_code = ?", String.class, status.code());
        assertThat(stored).doesNotContain(FIRST_NAME).doesNotContain("causa=").doesNotContain("fumava");
    }

    @Test
    @DisplayName("o banco recusa alterar o pedido travado, o detalhe e qualquer DELETE")
    void the_database_refuses_changes_to_a_locked_request() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());
        String code = status.code();

        assertThatThrownBy(() -> jdbc.update("UPDATE research.consultation_request SET commitment = ? WHERE code = ?",
                "0".repeat(64), code)).hasMessageContaining("travado");
        assertThatThrownBy(() -> jdbc.update("UPDATE research.consultation_request SET age_band = '18-39' WHERE code = ?",
                code)).hasMessageContaining("travado");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM research.consultation_request WHERE code = ?", code))
                .hasMessageContaining("nao pode ser apagado");
        assertThatThrownBy(() -> jdbc.update("UPDATE identity.consultation_detail SET answers_enc = 'x' WHERE request_code = ?",
                code)).hasMessageContaining("travado");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM identity.consultation_detail WHERE request_code = ?", code))
                .hasMessageContaining("nao pode ser apagado");
    }

    @Test
    @DisplayName("um pedido ativo por consulente; medium e conta nao confirmada nao fazem pedido")
    void who_can_file_a_request() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        consultations.submit(sitter, draft());
        assertThatThrownBy(() -> consultations.submit(sitter, draft()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already has");

        String medium = confirmedAccount(AccountProfile.MEDIUM);
        assertThatThrownBy(() -> consultations.submit(medium, draft()))
                .isInstanceOf(IllegalStateException.class);

        AccountRegistration unconfirmed = accounts.register(new AccountSignUp("Sem Confirmar",
                "pedido-" + UUID.randomUUID() + "@exemplo.test", "senha-de-teste-123", AccountProfile.SITTER,
                AccountService.CURRENT_TERM_VERSION, true));
        assertThatThrownBy(() -> consultations.submit(unconfirmed.code(), draft()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("cada consulente ve so o proprio pedido")
    void a_sitter_sees_only_their_own_request() {
        String first = confirmedAccount(AccountProfile.SITTER);
        String second = confirmedAccount(AccountProfile.SITTER);
        consultations.submit(first, draft());

        assertThat(consultations.own(second)).isEmpty();
        assertThat(consultations.status(second)).isEmpty();
    }

    @Test
    @DisplayName("rascunho invalido e recusado antes de gravar qualquer coisa")
    void invalid_drafts_store_nothing() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        assertThatThrownBy(() -> consultations.submit(sitter, new ConsultationDraft("", answers())))
                .hasMessageContaining("primeiro nome");
        assertThatThrownBy(() -> consultations.submit(sitter, new ConsultationDraft("R2D2", answers())))
                .hasMessageContaining("letras");
        Map<String, String> missingPairing = answers();
        missingPairing.put(Questionnaires.GENDER, Questionnaire.UNKNOWN);
        assertThatThrownBy(() -> consultations.submit(sitter, new ConsultationDraft(FIRST_NAME, missingPairing)))
                .hasMessageContaining("pares");
        assertThat(consultations.status(sitter)).isEmpty();
    }

    @Test
    @DisplayName("o administrador e avisado do pedido travado so pelo codigo")
    void the_administrator_is_notified() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        assertThat(notices.recent(30)).anySatisfy(notice -> {
            assertThat(notice.kind()).isEqualTo(NoticeKind.REQUEST_LOCKED);
            assertThat(notice.subjectCode()).isEqualTo(status.code());
        });
    }

    @Test
    @DisplayName("excluir a conta retira o pedido e apaga o detalhe cifrado")
    void closing_the_account_withdraws_the_request() {
        String sitter = confirmedAccount(AccountProfile.SITTER);
        ConsultationStatus status = consultations.submit(sitter, draft());

        accounts.close(sitter);

        assertThat(consultations.status(sitter)).isEmpty();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT d.first_name_enc, d.erased_at, r.state FROM identity.consultation_detail d "
                        + "JOIN research.consultation_request r ON r.code = d.request_code WHERE d.request_code = ?",
                status.code());
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("first_name_enc")).isEqualTo("");
            assertThat(row.get("erased_at")).isNotNull();
            assertThat(row.get("state")).isEqualTo("WITHDRAWN");
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit.event WHERE subject_ref = ? "
                + "AND event_type = 'CONSULTATION_WITHDRAWN'", Integer.class, "request:" + status.code())).isEqualTo(1);
    }
}
