package io.github.rogeriocisi.airlab.structured;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The P4 scoring rule (SPEC E13, "Conferência automática"), field by field. */
class StructuredScoringTest {

    private final Questionnaire questionnaire = Questionnaires.current();
    private final int scored = questionnaire.scoredFields().size();

    @Test
    @DisplayName("leituras iguais ao proprio alvo e diferentes do chamariz dao d igual ao dobro dos campos")
    void perfect_readings_give_twice_the_comparable_fields() {
        Map<String, String> a = P4Fixture.mediumAnswers(1);
        Map<String, String> b = P4Fixture.mediumAnswers(2);

        StructuredScoring.PairScore score = StructuredScoring.score(questionnaire, a, b, a, b);

        assertThat(score.comparableFields()).hasSize(scored);
        assertThat(score.targetMatchesA()).isEqualTo(scored);
        assertThat(score.decoyMatchesA()).isZero();
        assertThat(score.d()).isEqualTo(2 * scored);
        // Readings swapped: the same information, on the wrong target, gives the opposite sign.
        assertThat(StructuredScoring.score(questionnaire, a, b, b, a).d()).isEqualTo(-2 * scored);
    }

    @Test
    @DisplayName("nao sei de qualquer dos dois tira o campo da comparacao, para alvo e chamariz")
    void unknown_takes_the_field_out_for_both() {
        String field = questionnaire.scoredFields().get(0).key();
        Map<String, String> a = new HashMap<>(P4Fixture.mediumAnswers(1));
        Map<String, String> b = new HashMap<>(P4Fixture.mediumAnswers(2));
        b.put(field, Questionnaire.UNKNOWN);

        StructuredScoring.PairScore score = StructuredScoring.score(questionnaire, a, b, a, b);

        assertThat(score.comparableFields()).hasSize(scored - 1).doesNotContain(field);
        assertThat(score.d()).isEqualTo(2 * (scored - 1));
    }

    @Test
    @DisplayName("nao recebi nunca acerta, e respostas frequentes se cancelam entre alvo e chamariz")
    void not_received_never_matches_and_common_answers_cancel_out() {
        Map<String, String> a = P4Fixture.mediumAnswers(1);
        Map<String, String> nothing = new HashMap<>(a);
        nothing.replaceAll((key, value) -> Questionnaire.NOT_RECEIVED);
        assertThat(StructuredScoring.score(questionnaire, a, a, nothing, nothing).d()).isZero();
        assertThat(StructuredScoring.matches(questionnaire.scoredFields().stream().map(Questionnaire.Field::key).toList(),
                nothing, a)).isZero();

        // Both sitters gave the same answers: whatever the reading says, target and decoy tie.
        assertThat(StructuredScoring.score(questionnaire, a, a, a, a).d()).isZero();
    }
}
