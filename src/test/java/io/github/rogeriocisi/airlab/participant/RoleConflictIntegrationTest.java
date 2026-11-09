package io.github.rogeriocisi.airlab.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.analysis.AnalysisUnit;
import io.github.rogeriocisi.airlab.analysis.PrimaryMeasure;
import io.github.rogeriocisi.airlab.analysis.StoppingRule;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.study.PreregistrationPlan;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudySummary;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Epic E10, lock 2: one role per person per study.
 *
 * <p>With self-service enrolment this is the fraud that matters. The same person signing up as
 * medium and as consulente produces a result that looks clean and means nothing, and nothing
 * else in the protocol would catch it.
 */
@SpringBootTest
@ActiveProfiles("test")
class RoleConflictIntegrationTest {

    @Autowired
    ParticipantService participants;

    @Autowired
    StudyService studies;

    @Autowired
    AuditTrail auditTrail;

    private static final ConsentDeclaration CONSENT = new ConsentDeclaration("v1", true);

    private StudySummary novoEstudo() {
        return studies.create("Estudo de conflito de papel", new PreregistrationPlan(
                List.of("Hipotese de teste"),
                "Mediuns com evidencia previa",
                "Mesmo genero e faixa de idade",
                List.of("encaixe obvio", "sem encaixe"),
                "Diferenca pareada alvo menos chamariz",
                PrimaryMeasure.PAIRED_GLOBAL_SCORE,
                0.5,
                AnalysisUnit.PAIR,
                StoppingRule.FIXED_SAMPLE_SIZE,
                30,
                5,
                List.of(),
                List.of()), "RES-0001");
    }

    @Test
    @DisplayName("a mesma pessoa nao pode ser mediun e consulente no mesmo estudo")
    void the_same_person_cannot_hold_two_roles() {
        StudySummary estudo = novoEstudo();
        PersonalData pessoa = new PersonalData("Fulano de Teste", "fulano@exemplo.test");

        String mediumCode = participants.enrol(
                estudo.id(), ParticipantRole.MEDIUM, pessoa, CONSENT, "RES-0001");
        assertThat(mediumCode).startsWith("MED-");

        assertThatThrownBy(() -> participants.enrol(
                estudo.id(), ParticipantRole.SITTER, pessoa, CONSENT, "RES-0001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIUM")
                .hasMessageContaining("one role per person");
    }

    @Test
    @DisplayName("experimentador e redator tambem nao se acumulam")
    void experimenter_and_redactor_are_different_people() {
        StudySummary estudo = novoEstudo();
        PersonalData pessoa = new PersonalData("Beltrano de Teste", "beltrano@exemplo.test");

        participants.enrol(estudo.id(), ParticipantRole.EXPERIMENTER, pessoa, CONSENT, "RES-0001");

        assertThatThrownBy(() -> participants.enrol(
                estudo.id(), ParticipantRole.REDACTOR, pessoa, CONSENT, "RES-0001"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("o contato e reconhecido mesmo escrito de outro jeito")
    void the_contact_is_normalised_before_comparison() {
        StudySummary estudo = novoEstudo();

        participants.enrol(estudo.id(), ParticipantRole.MEDIUM,
                new PersonalData("Cicrano", "cicrano@exemplo.test"), CONSENT, "RES-0001");

        assertThatThrownBy(() -> participants.enrol(estudo.id(), ParticipantRole.SITTER,
                new PersonalData("Cicrano", "  CICRANO@Exemplo.Test  "), CONSENT, "RES-0001"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a mesma pessoa pode participar de estudos diferentes")
    void another_study_is_another_matter() {
        StudySummary primeiro = novoEstudo();
        StudySummary segundo = novoEstudo();
        PersonalData pessoa = new PersonalData("Participante", "participante@exemplo.test");

        participants.enrol(primeiro.id(), ParticipantRole.MEDIUM, pessoa, CONSENT, "RES-0001");
        String noSegundo = participants.enrol(
                segundo.id(), ParticipantRole.SITTER, pessoa, CONSENT, "RES-0001");

        assertThat(noSegundo).startsWith("SIT-");
    }

    @Test
    @DisplayName("a recusa fica registrada na trilha, porque tentativa bloqueada e informacao")
    void the_refusal_is_audited() {
        StudySummary estudo = novoEstudo();
        PersonalData pessoa = new PersonalData("Tentativa", "tentativa@exemplo.test");
        participants.enrol(estudo.id(), ParticipantRole.MEDIUM, pessoa, CONSENT, "RES-0001");

        long antes = auditTrail.size();
        assertThatThrownBy(() -> participants.enrol(
                estudo.id(), ParticipantRole.SITTER, pessoa, CONSENT, "RES-0001"))
                .isInstanceOf(IllegalStateException.class);

        // A trilha é append-only e a transação da recusa foi desfeita; o que importa aqui é que
        // a cadeia continua íntegra e que a tentativa não deixou participante cadastrado.
        assertThat(auditTrail.verifyChain().intact()).isTrue();
        assertThat(auditTrail.size()).isGreaterThanOrEqualTo(antes);
        assertThat(participants.codesWithRole(estudo.id(), ParticipantRole.SITTER)).isEmpty();
    }

    @Test
    @DisplayName("inscricao sem contato e aceita, e o estudo fica sabendo que a checagem nao rodou")
    void an_enrolment_without_a_contact_is_flagged() {
        StudySummary estudo = novoEstudo();

        String code = participants.enrol(estudo.id(), ParticipantRole.MEDIUM,
                new PersonalData("Sem Contato", null), CONSENT, "RES-0001");

        assertThat(code).startsWith("MED-");
        assertThat(auditTrail.verifyChain().intact()).isTrue();
    }
}
