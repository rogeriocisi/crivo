package io.github.rogeriocisi.airlab.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Epic E6 and E10: the link is the credential, so it behaves like one.
 */
class AccessLinkTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    @Test
    @DisplayName("o token sorteado abre o pacote dentro do prazo")
    void a_fresh_token_opens_the_packet() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(14));

        Optional<AccessLink.Refusal> refusal = AccessLink.refuse(
                issued.token(), issued.tokenHash(), issued.expiresAt(), null, NOW);

        assertThat(refusal).isEmpty();
        assertThat(issued.token()).hasSize(32);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
    }

    @Test
    @DisplayName("o token em claro nunca e igual ao que fica guardado")
    void only_the_hash_is_storable() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(1));

        assertThat(issued.tokenHash()).hasSize(64).isNotEqualTo(issued.token());
        assertThat(AccessLink.hash(issued.token())).isEqualTo(issued.tokenHash());
    }

    @Test
    @DisplayName("o token nao aparece no toString, para nao vazar em log")
    void the_token_does_not_leak_through_to_string() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(1));

        assertThat(issued.toString()).doesNotContain(issued.token()).contains("redacted");
    }

    @Test
    @DisplayName("token desconhecido e recusado sem dizer mais nada")
    void an_unknown_token_is_refused() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(1));

        assertThat(AccessLink.refuse("OUTROTOKEN", issued.tokenHash(), issued.expiresAt(), null, NOW))
                .contains(AccessLink.Refusal.UNKNOWN_TOKEN);
        assertThat(AccessLink.refuse(issued.token(), null, issued.expiresAt(), null, NOW))
                .contains(AccessLink.Refusal.UNKNOWN_TOKEN);
    }

    @Test
    @DisplayName("depois do prazo o link nao abre")
    void an_expired_link_is_refused() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(7));

        assertThat(AccessLink.refuse(issued.token(), issued.tokenHash(), issued.expiresAt(), null,
                issued.expiresAt()))
                .contains(AccessLink.Refusal.EXPIRED);
    }

    @Test
    @DisplayName("pacote ja enviado nunca reabre, nem dentro do prazo")
    void a_submitted_packet_never_reopens() {
        AccessLink.Issued issued = AccessLink.issue(NOW, Duration.ofDays(14));
        Instant submitted = NOW.plus(Duration.ofDays(2));

        assertThat(AccessLink.refuse(issued.token(), issued.tokenHash(), issued.expiresAt(),
                submitted, NOW.plus(Duration.ofDays(3))))
                .contains(AccessLink.Refusal.ALREADY_SUBMITTED);
    }

    @Test
    @DisplayName("dois links emitidos nunca coincidem")
    void links_are_unique() {
        assertThat(AccessLink.issue(NOW, Duration.ofDays(1)).token())
                .isNotEqualTo(AccessLink.issue(NOW, Duration.ofDays(1)).token());
    }

    @Test
    @DisplayName("prazo nao positivo e recusado")
    void a_link_needs_a_deadline() {
        assertThatThrownBy(() -> AccessLink.issue(NOW, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
