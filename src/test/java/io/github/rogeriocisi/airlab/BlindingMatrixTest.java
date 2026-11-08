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
