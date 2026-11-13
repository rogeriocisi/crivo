package io.github.rogeriocisi.airlab.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The P4 questionnaire (SPEC E13, annex): the shape the comparison depends on.
 */
class QuestionnaireTest {

    private final Questionnaire current = Questionnaires.current();

    private Map<String, String> fullAnswers() {
        Map<String, String> answers = new HashMap<>();
        for (Questionnaire.Field field : current.fields()) {
            answers.put(field.key(), field.options().get(0).key());
        }
        return answers;
    }

    @Test
    @DisplayName("os campos de pareamento sao os tres do P1 e ficam fora da nota")
    void pairing_fields_are_the_p1_ones_and_out_of_the_score() {
        List<String> pairing = current.fields().stream()
                .filter(field -> !field.scored()).map(Questionnaire.Field::key).toList();
        assertThat(pairing).containsExactlyInAnyOrder(
                Questionnaires.GENDER, Questionnaires.AGE_BAND, Questionnaires.DEATH_PERIOD);
        current.fields().stream().filter(field -> !field.scored())
                .forEach(field -> assertThat(field.outOfScoreReason()).contains("fora da nota"));
    }

    @Test
    @DisplayName("as faixas de pareamento tem o mesmo numero de opcoes das do P1")
    void pairing_bands_match_p1() {
        assertThat(current.field(Questionnaires.AGE_BAND).orElseThrow().options()).hasSize(5);
        assertThat(current.field(Questionnaires.DEATH_PERIOD).orElseThrow().options()).hasSize(4);
    }

    @Test
    @DisplayName("ha campos pontuaveis, e nenhum deles e texto livre")
    void there_are_scored_closed_fields() {
        assertThat(current.scoredFields()).hasSizeGreaterThanOrEqualTo(10);
        current.scoredFields().forEach(field -> assertThat(field.options()).hasSizeGreaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("resposta completa passa; falta, opcao desconhecida e campo a mais nao")
    void sitter_answers_are_checked() {
        assertThatCode(() -> current.checkSitterAnswers(fullAnswers())).doesNotThrowAnyException();

        Map<String, String> missing = fullAnswers();
        missing.remove("causa");
        assertThatThrownBy(() -> current.checkSitterAnswers(missing)).hasMessageContaining("Causa principal");

        Map<String, String> unknownOption = fullAnswers();
        unknownOption.put("causa", "abducao");
        assertThatThrownBy(() -> current.checkSitterAnswers(unknownOption)).hasMessageContaining("desconhecida");

        Map<String, String> extra = fullAnswers();
        extra.put("signo", "leao");
        assertThatThrownBy(() -> current.checkSitterAnswers(extra)).hasMessageContaining("desconhecido");
    }

    @Test
    @DisplayName("nao sei vale nos campos pontuaveis e nunca nos de pareamento")
    void unknown_is_allowed_only_where_pairing_does_not_need_it() {
        Map<String, String> unknownScored = fullAnswers();
        unknownScored.put("causa", Questionnaire.UNKNOWN);
        assertThatCode(() -> current.checkSitterAnswers(unknownScored)).doesNotThrowAnyException();

        Map<String, String> unknownPairing = fullAnswers();
        unknownPairing.put(Questionnaires.AGE_BAND, Questionnaire.UNKNOWN);
        assertThatThrownBy(() -> current.checkSitterAnswers(unknownPairing)).hasMessageContaining("pares");
    }

    @Test
    @DisplayName("as versoes sao localizaveis e as chaves de campo sao unicas")
    void versions_and_keys() {
        assertThat(Questionnaires.byVersion(current.version())).contains(current);
        assertThat(Questionnaires.byVersion("v0")).isEmpty();
        assertThatThrownBy(() -> new Questionnaire("x", List.of(
                current.scoredFields().get(0), current.scoredFields().get(0))))
                .hasMessageContaining("duplicate");
    }

    @Test
    @DisplayName("os rotulos de nao sei e nao recebi sao fixos")
    void labels() {
        assertThat(current.labelOf("causa", Questionnaire.UNKNOWN)).isEqualTo("Não sei");
        assertThat(current.labelOf("causa", Questionnaire.NOT_RECEIVED)).isEqualTo("Não recebi");
        assertThat(current.labelOf("causa", "cancer")).isEqualTo("Câncer");
    }

    // --- Fase C: as respostas do medium ---------------------------------------------------------

    private Map<String, String> mediumAnswers() {
        Map<String, String> answers = new HashMap<>();
        for (Questionnaire.Field field : current.scoredFields()) {
            answers.put(field.key(), field.options().get(0).key());
        }
        return answers;
    }

    @Test
    @DisplayName("o medium responde so aos campos da nota, cada um com uma opcao ou nao recebi")
    void medium_answers_only_the_scored_fields() {
        assertThatCode(() -> current.checkMediumAnswers(mediumAnswers())).doesNotThrowAnyException();

        Map<String, String> notReceived = mediumAnswers();
        notReceived.replaceAll((key, value) -> Questionnaire.NOT_RECEIVED);
        assertThatCode(() -> current.checkMediumAnswers(notReceived)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o medium nao deixa campo em branco, nao responde ao pareamento e nao usa nao sei")
    void medium_answers_are_complete_and_closed() {
        Map<String, String> missing = mediumAnswers();
        missing.remove(current.scoredFields().get(0).key());
        assertThatThrownBy(() -> current.checkMediumAnswers(missing))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Não recebi");

        Map<String, String> pairing = mediumAnswers();
        pairing.put(Questionnaires.GENDER, "feminino");
        assertThatThrownBy(() -> current.checkMediumAnswers(pairing))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Campo desconhecido");

        Map<String, String> unknown = mediumAnswers();
        unknown.put(current.scoredFields().get(0).key(), Questionnaire.UNKNOWN);
        assertThatThrownBy(() -> current.checkMediumAnswers(unknown))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Opção desconhecida");
    }
}
