package io.github.rogeriocisi.airlab.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.allocation.DrawInput;
import io.github.rogeriocisi.airlab.analysis.AnalysisUnit;
import io.github.rogeriocisi.airlab.analysis.PrimaryMeasure;
import io.github.rogeriocisi.airlab.analysis.StoppingRule;
import io.github.rogeriocisi.airlab.study.ProtocolDeviation;
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
 * Constitution P2 and P5, Epic E1 acceptance: once frozen, no field of the preregistration
 * can change, and the hash checked by the CLI matches the published document.
 */
@SpringBootTest
@ActiveProfiles("test")
class PreregistrationIntegrationTest {

    @Autowired
    StudyService studies;

    @Autowired
    AllocationService allocations;

    @Autowired
    DataSource dataSource;

    private static PreregistrationPlan plan() {
        return new PreregistrationPlan(
                List.of("Sitters escolhem a propria leitura acima do acaso"),
                "Mediuns com evidencia previa e consistente de AIR, triados antes do estudo",
                "Mesmo genero e mesma faixa de idade ao obito",
                List.of("encaixe obvio", "encaixe parcial ou interpretado", "sem encaixe",
                        "encaixa em outra pessoa", "nao sei avaliar"),
                "Diferenca pareada alvo menos chamariz na nota global, Wilcoxon unilateral",
                PrimaryMeasure.PAIRED_GLOBAL_SCORE,
                0.5,
                AnalysisUnit.PAIR,
                StoppingRule.FIXED_SAMPLE_SIZE,
                30,
                5,
                List.of("Proporcao de escolhas forcadas corretas, teste binomial"),
                List.of("Sessao interrompida e excluida"));
    }

    @Test
    @DisplayName("o estudo nasce com a versao 1 do pre-registro e o hash do documento confere")
    void study_starts_with_a_draft_version() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");

        assertThat(study.status()).isEqualTo(StudyStatus.DRAFT);
        PreregistrationSnapshot version = studies.currentPreregistration(study.id()).orElseThrow();
        assertThat(version.version()).isEqualTo(1);
        assertThat(version.frozen()).isFalse();
        assertThat(version.sha256()).isEqualTo(Hashes.sha256Hex(version.document()));
    }

    @Test
    @DisplayName("congelar exige o compromisso do sorteio e torna o documento imutavel")
    void freezing_binds_the_draw_and_locks_the_document() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");

        assertThatThrownBy(() -> studies.freeze(study.id(), "   ", "RES-0001"))
                .isInstanceOf(IllegalArgumentException.class);

        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2));

        PreregistrationSnapshot frozen = studies.freeze(study.id(), commitment, "RES-0001");

        assertThat(frozen.frozen()).isTrue();
        assertThat(frozen.document()).contains(commitment);
        assertThat(frozen.sha256()).isEqualTo(Hashes.sha256Hex(frozen.document()));
        assertThat(studies.byId(study.id()).orElseThrow().status())
                .isEqualTo(StudyStatus.PREREGISTERED);

        assertThatThrownBy(() -> studies.freeze(study.id(), commitment, "RES-0001"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("o banco recusa alterar uma versao congelada")
    void database_rejects_changing_a_frozen_version() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2));
        studies.freeze(study.id(), commitment, "RES-0001");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update(
                "update research.preregistration_version set document = 'outro' where study_id = ?",
                study.id()))
                .hasMessageContaining("immutable");
    }

    @Test
    @DisplayName("uma emenda cria a versao 2 e nao apaga a versao 1")
    void an_amendment_keeps_the_previous_version_visible() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2));
        PreregistrationSnapshot first = studies.freeze(study.id(), commitment, "RES-0001");

        studies.amend(study.id(), plan(), "Acrescimo de criterio de exclusao", "RES-0001");

        List<PreregistrationSnapshot> history = studies.preregistrationHistory(study.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).sha256()).isEqualTo(first.sha256());
        assertThat(history.get(1).amendmentReason()).isPresent();
    }

    @Test
    @DisplayName("um segundo sorteio no mesmo estudo e recusado: invalidaria o compromisso")
    void a_second_draw_is_refused() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        DrawInput input = new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2);
        allocations.draw(study.id(), input);

        assertThatThrownBy(() -> allocations.draw(study.id(), input))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("um desvio de protocolo e registrado, aparece na lista e nao pode ser apagado")
    void a_protocol_deviation_is_recorded_and_cannot_be_erased() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");

        ProtocolDeviation deviation = studies.recordDeviation(
                study.id(),
                ProtocolDeviation.Kind.SAMPLE_SIZE,
                "A coleta parou em 18 dos 30 pares por indisponibilidade de mediuns",
                java.time.Instant.parse("2026-11-01T12:00:00Z"),
                "RES-0001");

        assertThat(deviation.preregistrationVersion()).isEqualTo(1);
        assertThat(studies.deviations(study.id()))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.kind()).isEqualTo(ProtocolDeviation.Kind.SAMPLE_SIZE);
                    assertThat(d.description()).contains("18 dos 30");
                });

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update(
                "delete from research.protocol_deviation where study_id = ?", study.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update(
                "update research.protocol_deviation set description = 'nada' where study_id = ?",
                study.id()))
                .hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("o documento congelado declara a medida primaria e o poder do desenho")
    void the_frozen_document_states_the_measure_and_the_power() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2));

        PreregistrationSnapshot frozen = studies.freeze(study.id(), commitment, "RES-0001");

        assertThat(frozen.document())
                .contains("PAIRED_GLOBAL_SCORE")
                .contains("Poder estatistico declarado")
                .contains("Suficiente: sim")
                .contains("Diferenca maxima de itens");
    }

    @Test
    @DisplayName("a semente so aparece depois da revelacao, e o compromisso continua conferindo")
    void the_seed_shows_up_only_after_the_reveal() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1"),
                2));

        assertThat(allocations.snapshot(study.id()).orElseThrow().seedHex()).isEmpty();

        String seed = allocations.revealSeed(study.id(), "RES-0001");

        assertThat(seed).hasSize(64);
        assertThat(allocations.snapshot(study.id()).orElseThrow().seedHex()).contains(seed);
        assertThat(Hashes.sha256Hex(
                seed + "|" + allocations.snapshot(study.id()).orElseThrow().canonicalAllocations()))
                .isEqualTo(commitment);
    }
}
