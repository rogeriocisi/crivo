package io.github.rogeriocisi.airlab.anchor.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.anchor.AnchorCheck;
import io.github.rogeriocisi.airlab.anchor.AnchorOutcome;
import io.github.rogeriocisi.airlab.anchor.AnchorService;
import io.github.rogeriocisi.airlab.anchor.AnchorView;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditHead;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC E8, v0.15, acceptance: no stamp is stored without its token checked; the stamped hash is always
 * the hash of an event of the trail; a milestone is stamped at the next check; the CLI catches a token
 * that does not check out; the stamps are public.
 *
 * <p>On a database of its own, recreated on every run, because the policy depends on which stamps
 * exist; and against an authority of the test's own, so the signatures are real and the network is
 * not needed. Every test starts ten days after the previous one, so their stamps never interact.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "airlab.local-database.database-name=airlab_test_carimbos",
        "airlab.local-database.recreate-database=true"})
class AnchorIntegrationTest {

    static final TestAuthority AUTHORITY = TestAuthority.create("Crivo Integracao");
    static final TestAuthority FOREIGN = TestAuthority.create("Intrusa");

    enum Mode { GRANT, UNREACHABLE, REJECT, FOREIGN }

    static final AtomicReference<Mode> MODE = new AtomicReference<>(Mode.GRANT);
    static final AtomicReference<Instant> AUTHORITY_CLOCK = new AtomicReference<>();
    static final AtomicLong EPOCH = new AtomicLong();

    @DynamicPropertySource
    static void authority(DynamicPropertyRegistry registry) throws Exception {
        Path directory = AUTHORITY.write(Files.createTempDirectory("airlab-tsa-teste"));
        registry.add("airlab.timestamp.tsa-certificate", () -> directory.resolve("tsa.crt").toUri().toString());
        registry.add("airlab.timestamp.tsa-certificate-sha256", AUTHORITY::tsaSha256);
        registry.add("airlab.timestamp.ca-certificate", () -> directory.resolve("cacert.pem").toUri().toString());
        registry.add("airlab.timestamp.ca-certificate-sha256", AUTHORITY::caSha256);
        registry.add("airlab.timestamp.authority", () -> "Autoridade de teste");
    }

    @TestConfiguration
    static class TestTsa {
        @Bean
        @Primary
        TsaClient testTsaClient() {
            return request -> switch (MODE.get()) {
                case GRANT -> AUTHORITY.grant(request, AUTHORITY_CLOCK.get());
                case FOREIGN -> FOREIGN.grant(request, AUTHORITY_CLOCK.get());
                case REJECT -> AUTHORITY.reject();
                case UNREACHABLE -> throw new AnchorRejected(
                        io.github.rogeriocisi.airlab.anchor.AnchorOutcome.FailureReason.UNREACHABLE, "fora do ar no teste");
            };
        }
    }

    @Autowired
    AnchorService anchors;

    @Autowired
    AuditTrail trail;

    @Autowired
    DataSource dataSource;

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() {
        MODE.set(Mode.GRANT);
    }

    /** A start time ten days after the previous test's, so the stamps of two tests never interact. */
    private static Instant start() {
        return Instant.parse("2031-01-01T00:00:00Z").plus(Duration.ofDays(10 * EPOCH.incrementAndGet()));
    }

    private AnchorOutcome check(Instant at) {
        AUTHORITY_CLOCK.set(at);
        return anchors.anchorIfDue(at);
    }

    private AuditHead milestone() {
        trail.append(AuditEventType.PREREGISTRATION_FROZEN, ActorRole.SYSTEM, null, "study:teste",
                Map.of("documentSha256", Hashes.sha256Hex("documento " + System.nanoTime())));
        return trail.head().orElseThrow();
    }

    private void ordinaryEvent() {
        trail.append(AuditEventType.ACCOUNT_CREATED, ActorRole.SYSTEM, null, "account:teste", Map.of("n", 1));
    }

    @Test
    @DisplayName("um marco e carimbado na verificacao seguinte, sobre o hash da cabeca, e vai a trilha")
    void a_milestone_is_stamped_at_the_next_check() {
        Instant t = start();
        AuditHead head = milestone();

        AnchorOutcome outcome = check(t);

        assertThat(outcome.kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);
        AnchorView anchor = outcome.anchor().orElseThrow();
        assertThat(anchor.anchoredPosition()).isEqualTo(head.position());
        assertThat(anchor.anchoredHash()).isEqualTo(head.hash());
        assertThat(anchor.attestedAt()).isEqualTo(t);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> last = jdbc.queryForMap(
                "select event_type, payload from audit.event order by seq desc limit 1");
        assertThat(last.get("event_type")).isEqualTo("TIMESTAMP_ANCHORED");
        assertThat((String) last.get("payload")).contains(head.hash()).contains(anchor.code())
                .contains(anchor.responseSha256());

        // The stamp's own event is not a change: nothing more to stamp.
        assertThat(check(t.plusSeconds(60)).kind()).isEqualTo(AnchorOutcome.Kind.NOT_DUE);
    }

    @Test
    @DisplayName("fora dos marcos, uma trilha que mudou so e carimbada depois do intervalo")
    void an_ordinary_change_waits_for_the_interval() {
        Instant t = start();
        milestone();
        assertThat(check(t).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);

        ordinaryEvent();
        assertThat(check(t.plus(Duration.ofHours(1))).kind()).isEqualTo(AnchorOutcome.Kind.NOT_DUE);
        assertThat(check(t.plus(Duration.ofHours(24))).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);
    }

    @Test
    @DisplayName("trilha parada nao e carimbada de novo, nem depois do intervalo")
    void an_unchanged_trail_is_not_stamped_again() {
        Instant t = start();
        milestone();
        assertThat(check(t).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);

        assertThat(check(t.plus(Duration.ofDays(3))).kind()).isEqualTo(AnchorOutcome.Kind.NOT_DUE);
    }

    @Test
    @DisplayName("autoridade fora do ar: a falha fica na tabela, nao na trilha, e o servidor espera para tentar de novo")
    void a_failure_is_recorded_off_the_trail_and_retried_later() {
        Instant t = start();
        milestone();
        assertThat(check(t).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);

        milestone();
        long size = trail.size();
        MODE.set(Mode.UNREACHABLE);
        AnchorOutcome failed = check(t.plus(Duration.ofMinutes(1)));
        assertThat(failed.kind()).isEqualTo(AnchorOutcome.Kind.FAILED);
        assertThat(failed.reason()).contains(AnchorOutcome.FailureReason.UNREACHABLE);
        assertThat(trail.size()).isEqualTo(size);
        assertThat(anchors.status().failuresSinceLastGrant()).isEqualTo(1);

        MODE.set(Mode.GRANT);
        assertThat(check(t.plus(Duration.ofMinutes(6))).kind()).isEqualTo(AnchorOutcome.Kind.NOT_DUE);
        assertThat(check(t.plus(Duration.ofMinutes(17))).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);
        assertThat(anchors.status().failuresSinceLastGrant()).isZero();
    }

    @Test
    @DisplayName("resposta de outra autoridade ou recusa nao viram carimbo")
    void a_foreign_signature_or_a_refusal_is_never_stored_as_a_stamp() {
        Instant t = start();
        milestone();
        int granted = anchors.granted().size();

        MODE.set(Mode.FOREIGN);
        assertThat(check(t).reason()).contains(AnchorOutcome.FailureReason.INVALID_RESPONSE);
        MODE.set(Mode.REJECT);
        assertThat(check(t.plus(Duration.ofMinutes(20))).reason()).contains(AnchorOutcome.FailureReason.REJECTED);

        assertThat(anchors.granted()).hasSize(granted);
        MODE.set(Mode.GRANT);
        assertThat(check(t.plus(Duration.ofMinutes(40))).kind()).isEqualTo(AnchorOutcome.Kind.GRANTED);
    }

    @Test
    @DisplayName("o banco recusa alterar ou apagar um carimbo, e recusa hash que nao e o do evento")
    void the_database_refuses_rewrites_and_foreign_hashes() {
        Instant t = start();
        milestone();
        AnchorView anchor = check(t).anchor().orElseThrow();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbc.update(
                "update audit.timestamp_anchor set attested_at = now() where code = ?", anchor.code()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from audit.timestamp_anchor where code = ?", anchor.code()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update(
                "insert into audit.timestamp_anchor (code, anchored_seq, anchored_hash, authority_url, requested_at,"
                        + " status, failure_reason) values ('ANC-FORJADO', ?, ?, 'x', now(), 'FAILED', 'UNREACHABLE')",
                anchor.anchoredPosition(), "f".repeat(64)))
                .hasMessageContaining("nao e o do evento");
    }

    @Test
    @DisplayName("a verificacao da CLI aceita os carimbos legitimos e pega um token que carimba outro valor")
    void the_cli_check_catches_a_token_for_another_value() throws Exception {
        Instant t = start();
        milestone();
        AnchorView good = check(t).anchor().orElseThrow();

        // A row that passes every database check — real event, real hash — but whose token stamps
        // something else. Only re-checking the token catches it.
        AuditHead head = trail.head().orElseThrow();
        org.bouncycastle.tsp.TimeStampRequest other = TimestampProtocol.request(Hashes.sha256("outro".getBytes()));
        byte[] foreignToken = AUTHORITY.grant(other.getEncoded(), t);
        new JdbcTemplate(dataSource).update(
                "insert into audit.timestamp_anchor (code, anchored_seq, anchored_hash, authority_url, requested_at,"
                        + " status, attested_at, serial_hex, response_der, response_sha256)"
                        + " values ('ANC-TROCADO', ?, ?, 'x', ?, 'GRANTED', ?, '1', ?, ?)",
                head.position(), head.hash(), java.sql.Timestamp.from(t), java.sql.Timestamp.from(t), foreignToken,
                Hashes.toHex(Hashes.sha256(foreignToken)));

        java.util.List<AnchorCheck> checks = anchors.verifyAll();
        assertThat(checks).filteredOn(c -> c.code().equals(good.code())).singleElement()
                .satisfies(c -> assertThat(c.ok()).isTrue());
        assertThat(checks).filteredOn(c -> c.code().equals("ANC-TROCADO")).singleElement()
                .satisfies(c -> {
                    assertThat(c.tokenValid()).isFalse();
                    assertThat(c.detail()).contains("TOKEN NAO CONFERE");
                });
    }

    @Test
    @DisplayName("a pagina publica lista os carimbos e entrega o .tsr, sem login")
    void the_stamps_are_public() throws Exception {
        Instant t = start();
        milestone();
        AnchorView anchor = check(t).anchor().orElseThrow();
        byte[] stored = anchors.response(anchor.code()).orElseThrow();

        mvc.perform(get("/carimbos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(anchor.code())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(anchor.anchoredHash())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openssl ts -verify")));
        mvc.perform(get("/carimbos/{code}.tsr", anchor.code()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/timestamp-reply"))
                .andExpect(content().bytes(stored));
        mvc.perform(get("/carimbos/{code}.tsr", "ANC-NAOEXISTE")).andExpect(status().isNotFound());
    }
}
