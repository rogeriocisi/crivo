package io.github.rogeriocisi.airlab.participant.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The two linked-account signals before the blind index (SPEC E13, phase E). */
class AccountSignalsTest {

    @Test
    @DisplayName("e-mail canonico: caixa, apelido com + e, no Gmail, os pontos")
    void canonical_email() {
        assertThat(AccountSignals.canonicalEmail(" Jo.Ao+crivo@GoogleMail.com ")).isEqualTo("joao@gmail.com");
        assertThat(AccountSignals.canonicalEmail("joao@gmail.com")).isEqualTo("joao@gmail.com");
        assertThat(AccountSignals.canonicalEmail("Ana+teste@Exemplo.org")).isEqualTo("ana@exemplo.org");
        // Outside Gmail a dot can make a different mailbox, so it stays.
        assertThat(AccountSignals.canonicalEmail("a.b@exemplo.org")).isEqualTo("a.b@exemplo.org");
    }

    @Test
    @DisplayName("nome normalizado: sem caixa, acentos, pontuacao nem espacos extras; uma palavra so nao conta")
    void normalized_name() {
        assertThat(AccountSignals.normalizedName("  José   da Silva ")).contains("jose da silva");
        assertThat(AccountSignals.normalizedName("JOSE DA SILVA")).contains("jose da silva");
        assertThat(AccountSignals.normalizedName("Ana-Lúcia Souza")).contains("ana lucia souza");
        assertThat(AccountSignals.normalizedName("Maria")).isEmpty();
    }
}
