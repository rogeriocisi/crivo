package io.github.rogeriocisi.airlab.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** How the mailer picks its mode: the outbox, SMTP or nothing. */
class DefaultMailerTest {

    @Test
    @DisplayName("testes guardam sempre na caixa de saida, mesmo com SMTP no ambiente")
    void tests_always_keep_the_outbox() {
        assertThat(DefaultMailer.modeFor(true, false, true)).isEqualTo(DefaultMailer.Mode.LOCAL);
        assertThat(DefaultMailer.modeFor(true, true, true)).isEqualTo(DefaultMailer.Mode.LOCAL);
    }

    @Test
    @DisplayName("perfil local: caixa de saida por padrao, SMTP so quando configurado de proposito")
    void the_local_profile_opts_into_smtp() {
        assertThat(DefaultMailer.modeFor(false, true, false)).isEqualTo(DefaultMailer.Mode.LOCAL);
        assertThat(DefaultMailer.modeFor(false, true, true)).isEqualTo(DefaultMailer.Mode.SMTP);
    }

    @Test
    @DisplayName("producao: SMTP ou envio desativado, nunca a caixa de saida em memoria")
    void production_is_smtp_or_nothing() {
        assertThat(DefaultMailer.modeFor(false, false, true)).isEqualTo(DefaultMailer.Mode.SMTP);
        assertThat(DefaultMailer.modeFor(false, false, false)).isEqualTo(DefaultMailer.Mode.DISABLED);
    }
}
