package io.github.rogeriocisi.airlab.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.allocation.DrawInput;
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
                "Mesmo genero e mesma faixa de idade ao obito",
                List.of("correto", "parcialmente", "incorreto", "nao sei avaliar"),
                "Proporcao de escolhas corretas comparada a 0,5, teste binomial unilateral",
                List.of(),
                List.of("Sessao interrompida e excluida"),
                70);
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
                List.of("EXP-1")));

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
                List.of("EXP-1")));
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
                List.of("EXP-1")));
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
                List.of("EXP-1"));
        allocations.draw(study.id(), input);

        assertThatThrownBy(() -> allocations.draw(study.id(), input))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a semente so aparece depois da revelacao, e o compromisso continua conferindo")
    void the_seed_shows_up_only_after_the_reveal() {
        StudySummary study = studies.create("Estudo piloto", plan(), "RES-0001");
        String commitment = allocations.draw(study.id(), new DrawInput(
                study.code(),
                List.of(new DrawInput.PairInput("PR-0001", "TGT-A", "TGT-B")),
                List.of("MED-1"),
                List.of("EXP-1")));

        assertThat(allocations.snapshot(study.id()).orElseThrow().seedHex()).isEmpty();

        String seed = allocations.revealSeed(study.id(), "RES-0001");

        assertThat(seed).hasSize(64);
        assertThat(allocations.snapshot(study.id()).orElseThrow().seedHex()).contains(seed);
        assertThat(Hashes.sha256Hex(
                seed + "|" + allocations.snapshot(study.id()).orElseThrow().canonicalAllocations()))
                .isEqualTo(commitment);
    }
}
