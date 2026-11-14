package io.github.rogeriocisi.airlab.anchor.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Instant;
import org.bouncycastle.tsp.TimeStampRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RFC 3161 checks of SPEC E8, v0.15: nothing is stored unless the answer is granted, carries the very
 * hash and nonce that were sent, and is signed by the pinned authority certificate.
 */
class TimestampProtocolTest {

    private static final TestAuthority AUTHORITY = TestAuthority.create("Crivo Teste");
    private static final TestAuthority FOREIGN = TestAuthority.create("Outra");
    private static final Instant T = Instant.parse("2030-05-01T12:00:00Z");

    private static byte[] hash(String text) {
        return Hashes.sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("resposta concedida, com o mesmo hash e o mesmo nonce, assinada pela autoridade fixada: confere")
    void a_granted_answer_checks_out() throws Exception {
        TimeStampRequest request = TimestampProtocol.request(hash("cabeca"));

        TimestampProtocol.TokenInfo info = TimestampProtocol.checkResponse(
                AUTHORITY.grant(request.getEncoded(), T), request, AUTHORITY.trust());

        assertThat(info.attestedAt()).isEqualTo(T);
        assertThat(info.serialHex()).isNotBlank();
    }

    @Test
    @DisplayName("resposta a outro pedido (outro hash) e recusada")
    void an_answer_for_another_hash_is_refused() throws Exception {
        TimeStampRequest sent = TimestampProtocol.request(hash("cabeca"));
        TimeStampRequest other = TimestampProtocol.request(hash("outra cabeca"));

        assertThatThrownBy(() -> TimestampProtocol.checkResponse(
                AUTHORITY.grant(other.getEncoded(), T), sent, AUTHORITY.trust()))
                .isInstanceOf(AnchorRejected.class)
                .extracting(e -> ((AnchorRejected) e).reason()).isEqualTo(FailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("resposta com o mesmo hash mas outro nonce e recusada: nao e a resposta deste pedido")
    void an_answer_with_another_nonce_is_refused() throws Exception {
        byte[] head = hash("cabeca");
        TimeStampRequest sent = TimestampProtocol.request(head);
        TimeStampRequest replayed = TimestampProtocol.request(head);

        assertThatThrownBy(() -> TimestampProtocol.checkResponse(
                AUTHORITY.grant(replayed.getEncoded(), T), sent, AUTHORITY.trust()))
                .isInstanceOf(AnchorRejected.class)
                .extracting(e -> ((AnchorRejected) e).reason()).isEqualTo(FailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("token assinado por outra autoridade e recusado, mesmo valido por si")
    void a_token_from_another_authority_is_refused() throws Exception {
        TimeStampRequest request = TimestampProtocol.request(hash("cabeca"));

        assertThatThrownBy(() -> TimestampProtocol.checkResponse(
                FOREIGN.grant(request.getEncoded(), T), request, AUTHORITY.trust()))
                .isInstanceOf(AnchorRejected.class)
                .hasMessageContaining("certificado fixado");
    }

    @Test
    @DisplayName("recusa da autoridade e resposta ilegivel viram falhas distintas")
    void refusals_and_garbage_are_told_apart() {
        TimeStampRequest request = TimestampProtocol.request(hash("cabeca"));

        assertThatThrownBy(() -> TimestampProtocol.checkResponse(AUTHORITY.reject(), request, AUTHORITY.trust()))
                .extracting(e -> ((AnchorRejected) e).reason()).isEqualTo(FailureReason.REJECTED);
        assertThatThrownBy(() -> TimestampProtocol.checkResponse(new byte[]{1, 2, 3}, request, AUTHORITY.trust()))
                .extracting(e -> ((AnchorRejected) e).reason()).isEqualTo(FailureReason.INVALID_RESPONSE);
        assertThatThrownBy(() -> TimestampProtocol.checkResponse(new byte[0], request, AUTHORITY.trust()))
                .extracting(e -> ((AnchorRejected) e).reason()).isEqualTo(FailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("o token guardado confere de novo so com o hash certo, e qualquer byte adulterado o derruba")
    void a_stored_token_rechecks_only_for_its_hash() throws Exception {
        byte[] head = hash("cabeca");
        TimeStampRequest request = TimestampProtocol.request(head);
        byte[] stored = AUTHORITY.grant(request.getEncoded(), T);

        assertThat(TimestampProtocol.checkStored(stored, head, AUTHORITY.trust()).attestedAt()).isEqualTo(T);
        assertThatThrownBy(() -> TimestampProtocol.checkStored(stored, hash("outra"), AUTHORITY.trust()))
                .isInstanceOf(AnchorRejected.class);

        // The signature is at the end of the structure: flipping a byte there must break it.
        byte[] tampered = stored.clone();
        tampered[tampered.length - 5] ^= 0x01;
        assertThatThrownBy(() -> TimestampProtocol.checkStored(tampered, head, AUTHORITY.trust()))
                .isInstanceOf(AnchorRejected.class);
    }

    @Test
    @DisplayName("so um SHA-256 de 32 bytes vai a carimbo")
    void only_a_sha256_is_stamped() {
        assertThatThrownBy(() -> TimestampProtocol.request(new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
