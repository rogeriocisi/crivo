package io.github.rogeriocisi.airlab.trials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.support.Hashes;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The guarantees of E11 and E12 against a real PostgreSQL: the target does not exist before
 * the answer is locked, the seed stays sealed until the session closes, and a locked trial
 * cannot be rewritten.
 */
@SpringBootTest
@ActiveProfiles("test")
class TrialSessionIntegrationTest {

    private static final String NONCE = "22".repeat(16);

    @Autowired
    TrialSessionService service;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("a semente so e revelada no fim, e o compromisso publicado confere com ela")
    void the_seed_is_revealed_only_at_the_end() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 3);
        service.contributeEntropy(progress.sessionCode(), NONCE);

        assertThat(progress.seedCommitment()).hasSize(64);
        assertThat(service.outcome(progress.sessionCode())).isEmpty();
        assertThat(seedColumn(progress.sessionCode())).isNull();

        for (int i = 0; i < 3; i++) {
            service.submitChoice(progress.sessionCode(), 1);
        }
        SessionOutcome outcome = service.finish(progress.sessionCode());

        assertThat(outcome.seedHex()).hasSize(64);
        assertThat(Hashes.sha256Hex(String.join("|", outcome.serverSeedHex(),
                TrialProtocol.PRECOGNITIVE_FORCED_CHOICE.name(), "3", "4", TargetPool.VERSION)))
                .isEqualTo(outcome.seedCommitment());
    }

    @Test
    @DisplayName("com a semente revelada, qualquer pessoa reproduz os alvos gravados")
    void the_revealed_seed_reproduces_every_target() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 6);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        for (int i = 0; i < 6; i++) {
            service.submitChoice(progress.sessionCode(), (i % 4) + 1);
        }
        SessionOutcome outcome = service.finish(progress.sessionCode());

        assertThat(outcome.trials()).hasSize(6);
        for (TrialOutcome trial : outcome.trials()) {
            int recomputed = TargetDerivation.targetIndex(outcome.seedHex(), trial.index(), 4) + 1;
            assertThat(outcome.seedHex()).isEqualTo(TargetDerivation.effectiveSeedHex(
                    outcome.serverSeedHex(), outcome.participantNonce()));
            assertThat(recomputed)
                    .describedAs("alvo da tentativa %s", trial.index())
                    .isEqualTo(trial.targetIndex());
            assertThat(trial.hit()).isEqualTo(trial.chosenIndex() == trial.targetIndex());
        }
        assertThat(outcome.hits())
                .isEqualTo((int) outcome.trials().stream().filter(TrialOutcome::hit).count());
    }

    @Test
    @DisplayName("durante a sessao o progresso traz contagem, nunca desempenho")
    void progress_carries_no_outcome() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 4);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 2);

        SessionProgress after = service.progress(progress.sessionCode());
        assertThat(after.lockedTrials()).isEqualTo(1);
        assertThat(after.remainingTrials()).isEqualTo(3);
        assertThat(after.finished()).isFalse();
        // No outcome is reachable while the session runs, by any path.
        assertThat(service.outcome(progress.sessionCode())).isEmpty();
    }

    @Test
    @DisplayName("sem a parte do participante nao existe tentativa, nem no codigo nem no banco")
    void nothing_happens_before_the_participant_contributes() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 4);

        assertThat(progress.awaitingEntropy()).isTrue();
        assertThatThrownBy(() -> service.submitChoice(progress.sessionCode(), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("parte do participante");

        // E a trava nao e so do codigo: o banco recusa a tentativa enquanto o nonce nao chegou.
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update(
                "insert into research.test_trial (session_code, trial_index) values (?, 1)",
                progress.sessionCode()))
                .hasMessageContaining("participant nonce");

        SessionProgress depois = service.contributeEntropy(progress.sessionCode(), NONCE);
        assertThat(depois.awaitingEntropy()).isFalse();
        assertThat(service.submitChoice(progress.sessionCode(), 1).lockedTrials()).isEqualTo(1);
    }

    @Test
    @DisplayName("a parte do participante e aceita uma vez so, e precisa ser hexadecimal")
    void the_participant_half_is_accepted_once() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 2);

        assertThatThrownBy(() -> service.contributeEntropy(progress.sessionCode(), "nao-hex"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.contributeEntropy(progress.sessionCode(), "aabb"))
                .isInstanceOf(IllegalArgumentException.class);

        service.contributeEntropy(progress.sessionCode(), NONCE);
        assertThatThrownBy(() -> service.contributeEntropy(progress.sessionCode(), "33".repeat(16)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja recebeu");
    }

    @Test
    @DisplayName("a mesma parte do participante com servidores diferentes da sorteios diferentes")
    void the_server_cannot_reuse_a_draw() {
        SessionProgress primeira = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 2);
        service.contributeEntropy(primeira.sessionCode(), NONCE);
        service.submitChoice(primeira.sessionCode(), 1);
        service.submitChoice(primeira.sessionCode(), 1);
        SessionOutcome uma = service.finish(primeira.sessionCode());

        SessionProgress segunda = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 2);
        service.contributeEntropy(segunda.sessionCode(), NONCE);
        service.submitChoice(segunda.sessionCode(), 1);
        service.submitChoice(segunda.sessionCode(), 1);
        SessionOutcome outra = service.finish(segunda.sessionCode());

        assertThat(outra.participantNonce()).isEqualTo(uma.participantNonce());
        assertThat(outra.serverSeedHex()).isNotEqualTo(uma.serverSeedHex());
        assertThat(outra.seedHex()).isNotEqualTo(uma.seedHex());
    }

    @Test
    @DisplayName("o alvo nao existe no banco antes de a resposta estar travada")
    void the_target_does_not_exist_before_the_answer_is_locked() {
        SessionProgress progress = service.start(TrialProtocol.REMOTE_VIEWING_RANKED, 1);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.lockImpression(progress.sessionCode(), "linhas paralelas, metal frio");

        // The trial row exists, with the impression locked and the candidates drawn, and the
        // target column is still empty: it is written only when the ranking is locked (V15).
        assertThat(targetColumn(progress.sessionCode(), 1)).isNull();

        service.submitRanking(progress.sessionCode(), List.of(2, 1, 3, 4));
        assertThat(targetColumn(progress.sessionCode(), 1)).isNotNull();
    }

    @Test
    @DisplayName("fechar antes do alvo declarado e recusado; para encerrar, abandona-se")
    void finishing_short_of_the_declared_trials_is_refused() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 5);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 1);

        assertThatThrownBy(() -> service.finish(progress.sessionCode()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("abandonada");

        SessionOutcome outcome = service.abandon(progress.sessionCode());
        assertThat(outcome.state()).isEqualTo(SessionState.ABANDONED);
        assertThat(outcome.declaredTrials()).isEqualTo(5);
        assertThat(outcome.lockedTrials()).isEqualTo(1);
        assertThat(outcome.seedHex()).hasSize(64);
    }

    @Test
    @DisplayName("passar do numero declarado de tentativas e recusado")
    void submitting_past_the_declared_trials_is_refused() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 2);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 1);
        service.submitChoice(progress.sessionCode(), 1);

        assertThatThrownBy(() -> service.submitChoice(progress.sessionCode(), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("na visao remota, a impressao e travada antes de as figuras aparecerem")
    void the_impression_is_locked_before_the_candidates_appear() {
        SessionProgress progress = service.start(TrialProtocol.REMOTE_VIEWING_RANKED, 2);
        service.contributeEntropy(progress.sessionCode(), NONCE);

        assertThatThrownBy(() -> service.submitRanking(
                progress.sessionCode(), List.of(1, 2, 3, 4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("impressao");

        TrialCandidates candidates =
                service.lockImpression(progress.sessionCode(), "algo alto, vertical, com luz");
        assertThat(candidates.candidates()).hasSize(4);
        assertThat(candidates.targetPoolVersion()).isEqualTo(TargetPool.VERSION);

        assertThatThrownBy(() -> service.lockImpression(progress.sessionCode(), "outra coisa"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reescrita");
    }

    @Test
    @DisplayName("a ordenacao precisa ser uma permutacao das quatro posicoes")
    void the_ranking_must_be_a_permutation() {
        SessionProgress progress = service.start(TrialProtocol.REMOTE_VIEWING_RANKED, 1);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.lockImpression(progress.sessionCode(), "formas redondas, agua");

        assertThatThrownBy(() -> service.submitRanking(progress.sessionCode(), List.of(1, 1, 2, 3)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submitRanking(progress.sessionCode(), List.of(1, 2, 3)))
                .isInstanceOf(IllegalArgumentException.class);

        SessionProgress after = service.submitRanking(progress.sessionCode(), List.of(3, 1, 4, 2));
        assertThat(after.lockedTrials()).isEqualTo(1);

        SessionOutcome outcome = service.finish(progress.sessionCode());
        TrialOutcome trial = outcome.trials().get(0);
        assertThat(trial.chosenIndex()).isEqualTo(3);
        assertThat(trial.candidateSlugs()).hasSize(4);
        assertThat(trial.impressionSha256())
                .contains(Hashes.sha256Hex("formas redondas, agua"));
    }

    @Test
    @DisplayName("o banco recusa reescrever uma tentativa travada")
    void the_database_refuses_to_rewrite_a_locked_trial() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 1);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 1);
        service.finish(progress.sessionCode());

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update(
                "update research.test_trial set chosen_position = 2 where session_code = ?",
                progress.sessionCode()))
                .hasMessageContaining("immutable");
    }

    @Test
    @DisplayName("fechar duas vezes devolve o mesmo desfecho, sem sortear de novo")
    void finishing_twice_is_idempotent() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 2);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 1);
        service.submitChoice(progress.sessionCode(), 2);

        SessionOutcome first = service.finish(progress.sessionCode());
        SessionOutcome second = service.finish(progress.sessionCode());

        assertThat(second.seedHex()).isEqualTo(first.seedHex());
        assertThat(second.hits()).isEqualTo(first.hits());
        assertThat(second.closedAt()).isEqualTo(first.closedAt());
    }

    @Test
    @DisplayName("o desfecho vem rotulado como exploratorio, com a razao")
    void the_outcome_is_labelled_exploratory() {
        SessionProgress progress = service.start(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE, 1);
        service.contributeEntropy(progress.sessionCode(), NONCE);
        service.submitChoice(progress.sessionCode(), 1);

        SessionOutcome outcome = service.finish(progress.sessionCode());
        assertThat(outcome.exploratoryReason()).contains("exploratórios");
        assertThat(outcome.chanceNumerator()).isEqualTo(1);
        assertThat(outcome.chanceDenominator()).isEqualTo(4);
    }

    private String seedColumn(String sessionCode) {
        return new JdbcTemplate(dataSource).queryForObject(
                "select seed_hex from research.test_session where code = ?",
                String.class, sessionCode);
    }

    private Integer targetColumn(String sessionCode, int trialIndex) {
        return new JdbcTemplate(dataSource).queryForObject(
                "select target_position from research.test_trial "
                        + "where session_code = ? and trial_index = ?",
                Integer.class, sessionCode, trialIndex);
    }
}
