package io.github.rogeriocisi.airlab.redaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Blinding matrix, redactor row: a redacted reading reaches the sitter with no first name
 * and no session clue (SPEC section 4, Epic E5 acceptance).
 */
class ClueScrubberTest {

    private static final List<String> PAIR_NAMES = List.of("Antônio", "Benedito");

    @Test
    @DisplayName("uma lista limpa passa")
    void clean_items_pass() {
        List<String> problems = ClueScrubber.inspect(List.of(
                "Era um homem alto, de maos grandes",
                "Gostava de mexer com marcenaria",
                "Mostra um relogio de bolso como algo importante"), PAIR_NAMES);

        assertThat(problems).isEmpty();
    }

    @Test
    @DisplayName("o primeiro nome do alvo e recusado, com ou sem acento")
    void rejects_the_target_first_name_regardless_of_accent() {
        assertThat(ClueScrubber.inspect(List.of("Antonio gostava de marcenaria"), PAIR_NAMES))
                .anyMatch(p -> p.contains("primeiro nome"));
        assertThat(ClueScrubber.inspect(List.of("Falava muito do ANTÔNIO"), PAIR_NAMES))
                .anyMatch(p -> p.contains("primeiro nome"));
    }

    @Test
    @DisplayName("o nome do chamariz tambem e recusado: dentro do par, ele revela a leitura")
    void rejects_the_decoy_first_name_too() {
        assertThat(ClueScrubber.inspect(List.of("Lembrou de Benedito na infancia"), PAIR_NAMES))
                .anyMatch(p -> p.contains("primeiro nome"));
    }

    @Test
    @DisplayName("um nome que apenas contem a sequencia de letras nao e pista")
    void does_not_flag_a_substring_that_is_not_a_word() {
        assertThat(ClueScrubber.inspect(List.of("Trabalhava em Antoniopolisia"), List.of("Antonio")))
                .isEmpty();
    }

    @Test
    @DisplayName("pistas de sessao sao recusadas")
    void rejects_session_clues() {
        assertThat(ClueScrubber.inspect(List.of("O experimentador repetiu a pergunta"), PAIR_NAMES))
                .anyMatch(p -> p.contains("pista de sessao"));
    }

    @Test
    @DisplayName("datas de sessao sao recusadas")
    void rejects_dates() {
        assertThat(ClueScrubber.inspect(List.of("Faleceu em 1998"), PAIR_NAMES))
                .anyMatch(p -> p.contains("data"));
        assertThat(ClueScrubber.inspect(List.of("Sessao de 12/03 pela manha"), PAIR_NAMES))
                .isNotEmpty();
    }

    @Test
    @DisplayName("item com mais de uma afirmacao e recusado (um item = uma afirmacao)")
    void rejects_items_with_more_than_one_statement() {
        assertThat(ClueScrubber.inspect(
                List.of("Era alto. Gostava de musica."), PAIR_NAMES))
                .anyMatch(p -> p.contains("mais de uma afirmacao"));
    }

    @Test
    @DisplayName("item vazio e recusado")
    void rejects_empty_items() {
        assertThat(ClueScrubber.inspect(List.of("   "), PAIR_NAMES)).anyMatch(p -> p.contains("vazio"));
    }
}
