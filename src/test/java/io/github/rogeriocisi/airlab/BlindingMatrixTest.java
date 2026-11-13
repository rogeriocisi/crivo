package io.github.rogeriocisi.airlab;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rogeriocisi.airlab.allocation.AllocationPlan;
import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.participant.TargetProfile;
import io.github.rogeriocisi.airlab.study.PreregistrationSnapshot;
import io.github.rogeriocisi.airlab.study.StudySummary;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Blinding matrix as a test suite (SPEC section 4, Constitution P1).
 *
 * <p>These check the structural half of the matrix: a field forbidden to a role is <em>absent
 * from the projection</em> rather than filtered in a view (architectural rule 3). Rows that
 * depend on persisted state are covered by the integration tests of each module as it lands;
 * the ones below hold for every build.
 */
class BlindingMatrixTest {

    private static final String[] IDENTIFYING_NAMES = {
            "name", "fullname", "firstname", "contact", "email", "phone", "notes"};

    @Test
    @DisplayName("a projecao de alvo usada no pareamento nao tem nenhum campo identificavel")
    void target_projection_carries_no_identifiable_field() {
        assertNoFieldsNamedLike(TargetProfile.class, IDENTIFYING_NAMES);
    }

    @Test
    @DisplayName("a visao de estudo do pesquisador nao expoe ligacao leitura -> alvo nem semente")
    void researcher_study_projection_has_no_blinding_sensitive_field() {
        assertNoFieldsNamedLike(StudySummary.class, "seed", "target", "reading", "sitter", "medium");
        assertNoFieldsNamedLike(PreregistrationSnapshot.class, "seed", "allocation", "target");
    }

    @Test
    @DisplayName("nenhum metodo publico do servico de sorteio devolve o plano de alocacao")
    void allocation_service_never_hands_out_the_plan() {
        for (Method method : AllocationService.class.getMethods()) {
            assertThat(method.getReturnType())
                    .describedAs("metodo %s devolveria o plano de alocacao", method.getName())
                    .isNotEqualTo(AllocationPlan.class);
            assertThat(method.getGenericReturnType().getTypeName())
                    .describedAs("metodo %s devolveria o plano de alocacao", method.getName())
                    .doesNotContain(AllocationPlan.class.getName());
        }
    }

    @Test
    @DisplayName("o progresso da coleta nao carrega nenhum campo de desfecho")
    void progress_carries_no_outcome_field() {
        // Epico E10, trava 1: automatizar o estudo poe este numero na frente do pesquisador
        // todos os dias. Se ele vier acompanhado do resultado, a parada opcional deixa de ser
        // uma tentacao ocasional e vira rotina.
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.analysis.CollectionProgress.class,
                "hit", "acerto", "score", "nota", "accuracy", "acuracia", "proportion",
                "proporcao", "pvalue", "correct", "result", "effect", "difference", "diferenca");
    }

    @Test
    @DisplayName("o resumo mostrado ao pesquisador conta unidades, nunca desempenho")
    void progress_summary_reports_counts_only() {
        io.github.rogeriocisi.airlab.analysis.CollectionProgress progress =
                new io.github.rogeriocisi.airlab.analysis.CollectionProgress(1L, 18, 30);

        assertThat(progress.remainingUnits()).isEqualTo(12);
        assertThat(progress.targetReached()).isFalse();
        assertThat(progress.summary())
                .contains("18 de 30")
                .contains("Nenhum resultado");
    }

    @Test
    @DisplayName("dados pessoais nao vazam por toString")
    void personal_data_does_not_leak_through_to_string() {
        assertThat(new io.github.rogeriocisi.airlab.participant.PersonalData("Maria", "maria@exemplo")
                .toString())
                .doesNotContain("Maria")
                .doesNotContain("maria@exemplo");
        assertThat(new io.github.rogeriocisi.airlab.participant.TargetRegistration(
                "Antonio",
                io.github.rogeriocisi.airlab.participant.TargetRegistration.Gender.MASCULINE,
                io.github.rogeriocisi.airlab.participant.TargetRegistration.AgeBand.FROM_60_TO_79,
                io.github.rogeriocisi.airlab.participant.TargetRegistration.DeathPeriod.FROM_1_TO_5_YEARS,
                null).toString())
                .doesNotContain("Antonio");
    }

    @Test
    @DisplayName("o progresso de uma sessao de teste nao tem nenhum campo de desempenho")
    void trial_session_progress_has_no_outcome_field() {
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.trials.SessionProgress.class,
                "hit", "score", "target", "correct", "acerto", "seedhex", "result");
    }

    // --- P4, leitura estruturada automatizada (SPEC E13, matriz de visibilidade do P4) ---------

    private static final String[] P4_CONTENT_AND_OUTCOME = {
            "name", "firstname", "answer", "resposta", "salt", "score", "nota", "hit", "acerto",
            "difference", "diferenca", "result", "email"};

    @Test
    @DisplayName("P4: o que o administrador ve tem contagens, codigos e horas, nunca conteudo ou nota")
    void p4_administrator_projections_carry_no_content_or_outcome() {
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.AccountCounts.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.ConsultationCounts.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.notification.AdminNotice.class, P4_CONTENT_AND_OUTCOME);
    }

    @Test
    @DisplayName("P4: a situacao do pedido diz onde ele esta, sem primeiro nome, respostas ou nota")
    void p4_request_status_carries_no_content_or_outcome() {
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.ConsultationStatus.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.ConsultationLocked.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.AccountConfirmed.class, P4_CONTENT_AND_OUTCOME);
    }

    @Test
    @DisplayName("P4: nenhum estado do pedido carrega desempenho")
    void p4_request_states_carry_no_outcome() {
        for (io.github.rogeriocisi.airlab.participant.ConsultationState state
                : io.github.rogeriocisi.airlab.participant.ConsultationState.values()) {
            assertThat(state.label().toLowerCase(Locale.ROOT))
                    .doesNotContain("acert").doesNotContain("nota").doesNotContain("encaix");
        }
    }

    @Test
    @DisplayName("P4: o pedido, a conta e o rascunho nao vazam por toString")
    void p4_content_does_not_leak_through_to_string() {
        java.util.Map<String, String> answers = java.util.Map.of("causa", "cancer");
        assertThat(new io.github.rogeriocisi.airlab.participant.ConsultationDraft("Sebastiao", answers).toString())
                .doesNotContain("Sebastiao").doesNotContain("cancer");
        assertThat(new io.github.rogeriocisi.airlab.participant.OwnConsultation("PED-1", "v1", "Sebastiao",
                answers, "0".repeat(64), java.time.Instant.EPOCH).toString())
                .doesNotContain("Sebastiao").doesNotContain("cancer");
        assertThat(new io.github.rogeriocisi.airlab.participant.AccountSignUp("Maria", "maria@exemplo",
                "segredo-123456", io.github.rogeriocisi.airlab.participant.AccountProfile.SITTER, "conta-v1", true)
                .toString())
                .doesNotContain("Maria").doesNotContain("maria@exemplo").doesNotContain("segredo");
    }

    // --- P4, fase C: o medium e o envio (SPEC E13, matriz de visibilidade do P4) ----------------

    /** What the medium must not get: the sitter, the request, anyone's answers, any score. */
    private static final String[] P4_MEDIUM_FORBIDDEN = {
            "sitter", "consulente", "request", "pedido", "answer", "resposta", "salt", "score", "nota",
            "hit", "acerto", "difference", "diferenca", "result", "email", "commitment"};

    @Test
    @DisplayName("P4: o que o medium ve do pedido tem nomes, posicao e prazo; nunca consulente, respostas ou nota")
    void p4_medium_projection_carries_no_sitter_link_answers_or_outcome() {
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.MediumAssignment.class, P4_MEDIUM_FORBIDDEN);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.MediumAssignment.Slot.class, P4_MEDIUM_FORBIDDEN);
    }

    @Test
    @DisplayName("P4: o progresso do estudo e so contagem, sem medium, par ou conteudo")
    void p4_study_progress_is_counts_only() {
        for (Class<?> type : new Class<?>[]{io.github.rogeriocisi.airlab.structured.StructuredProgress.class,
                io.github.rogeriocisi.airlab.structured.CycleReport.class}) {
            Arrays.stream(type.getRecordComponents()).forEach(component -> assertThat(component.getType())
                    .describedAs("%s.%s", type.getSimpleName(), component.getName())
                    .isIn(int.class, long.class));
        }
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.StructuredStudyView.class, P4_CONTENT_AND_OUTCOME);
        // N is a count; what must be absent is any code of a pair, a send, a medium or a reading.
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.StructuredStudyView.class,
                "medium", "paircode", "assignment", "reading", "leitura");
    }

    @Test
    @DisplayName("P4: a API do estudo nao devolve a ligacao par -> medium, nem leitura, a quem a usa")
    void p4_study_service_returns_no_pair_medium_link_or_reading() {
        for (Method method : io.github.rogeriocisi.airlab.structured.StructuredStudyService.class.getMethods()) {
            assertThat(method.getReturnType())
                    .describedAs("StructuredStudyService.%s", method.getName())
                    .isIn(void.class, boolean.class, java.util.Optional.class,
                            io.github.rogeriocisi.airlab.structured.StructuredStudyView.class,
                            io.github.rogeriocisi.airlab.structured.StructuredProgress.class,
                            io.github.rogeriocisi.airlab.structured.CycleReport.class);
        }
    }

    @Test
    @DisplayName("P4: o pedido visto pelo pareamento e os eventos do envio nao carregam nome, resposta ou nota")
    void p4_pairing_view_and_events_carry_no_content() {
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.participant.ConsultationRef.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.ReadingRequestSent.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.PairReadingsReceived.class, P4_CONTENT_AND_OUTCOME);
        assertNoFieldsNamedLike(io.github.rogeriocisi.airlab.structured.ReadingRequestExpired.class, P4_CONTENT_AND_OUTCOME);
    }

    @Test
    @DisplayName("P4: o consulente nao recebe a leitura nem o medium antes do desmascaramento")
    void p4_sitter_projections_carry_no_reading_or_medium() {
        for (Class<?> type : new Class<?>[]{io.github.rogeriocisi.airlab.participant.ConsultationStatus.class,
                io.github.rogeriocisi.airlab.participant.OwnConsultation.class}) {
            assertNoFieldsNamedLike(type, "reading", "leitura", "medium", "score", "nota", "hit", "acerto");
        }
    }

    @Test
    @DisplayName("P4: nenhum estado de envio ao medium carrega desempenho")
    void p4_assignment_states_carry_no_outcome() {
        for (io.github.rogeriocisi.airlab.structured.AssignmentState state
                : io.github.rogeriocisi.airlab.structured.AssignmentState.values()) {
            assertThat(state.label().toLowerCase(Locale.ROOT))
                    .doesNotContain("acert").doesNotContain("nota").doesNotContain("encaix");
        }
    }

    @Test
    @DisplayName("P4: o envio e a leitura do medium nao vazam por toString")
    void p4_medium_content_does_not_leak_through_to_string() {
        var slot = new io.github.rogeriocisi.airlab.structured.MediumAssignment.Slot(1, "Sebastiao", false, null);
        var assignment = new io.github.rogeriocisi.airlab.structured.MediumAssignment("ENV-1",
                io.github.rogeriocisi.airlab.structured.AssignmentState.OPEN, "v1", java.time.Instant.EPOCH,
                java.time.Instant.EPOCH.plusSeconds(60), java.util.List.of(slot));
        assertThat(assignment.toString()).doesNotContain("Sebastiao");
        assertThat(slot.toString()).doesNotContain("Sebastiao");
        assertThat(new io.github.rogeriocisi.airlab.structured.MediumReadingDraft("ENV-1", 1,
                java.util.Map.of("causa", "cancer")).toString()).doesNotContain("cancer");
    }

    private static void assertNoFieldsNamedLike(Class<?> type, String... forbidden) {
        Arrays.stream(type.getRecordComponents()).forEach(component -> {
            String name = component.getName().toLowerCase(Locale.ROOT);
            for (String word : forbidden) {
                assertThat(name)
                        .describedAs("%s.%s nao deveria existir nesta projecao",
                                type.getSimpleName(), component.getName())
                        .doesNotContain(word);
            }
        });
    }
}
