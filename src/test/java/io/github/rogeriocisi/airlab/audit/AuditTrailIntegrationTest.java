package io.github.rogeriocisi.airlab.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Constitution P4, Epic E8 acceptance: "manually changing any record in the database is
 * detected by the CLI". These tests run against a real PostgreSQL, because the guarantee
 * lives in database triggers, not in Java.
 *
 * <p>Never disable or skip these (CLAUDE.md, working rules).
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    @Autowired
    AuditTrail auditTrail;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("a cadeia de eventos se mantem verificavel a cada evento gravado")
    void chain_stays_verifiable() {
        long before = auditTrail.size();
        String first = auditTrail.append(AuditEventType.STUDY_CREATED, ActorRole.RESEARCHER,
                "RES-0001", "study:1", Map.of("study", "STD-0001"));
        String second = auditTrail.append(AuditEventType.PAIRS_FORMED, ActorRole.SYSTEM,
                null, "study:1", Map.of("pairs", 3));

        assertThat(first).hasSize(64);
        assertThat(second).hasSize(64).isNotEqualTo(first);
        assertThat(auditTrail.size()).isEqualTo(before + 2);
        assertThat(auditTrail.headHash()).isEqualTo(second);

        ChainVerification verification = auditTrail.verifyChain();
        assertThat(verification.intact())
                .describedAs(verification.detail())
                .isTrue();
        assertThat(verification.checkedEvents()).isEqualTo(auditTrail.size());
    }

    @Test
    @DisplayName("o banco recusa UPDATE na trilha, mesmo vindo do usuario da aplicacao")
    void database_rejects_update() {
        auditTrail.append(AuditEventType.SESSION_SCHEDULED, ActorRole.RESEARCHER, "RES-0001",
                "study:1", Map.of("sessions", 2));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbc.update(
                "update audit.event set payload = '{}' where seq = (select max(seq) from audit.event)"))
                .hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("o banco recusa DELETE na trilha")
    void database_rejects_delete() {
        auditTrail.append(AuditEventType.SESSION_SCHEDULED, ActorRole.RESEARCHER, "RES-0001",
                "study:1", Map.of("sessions", 2));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbc.update(
                "delete from audit.event where seq = (select max(seq) from audit.event)"))
                .hasMessageContaining("append-only");
    }

    @Test
    @Transactional
    @DisplayName("um evento com payload adulterado e detectado pela verificacao")
    void tampering_is_detected() {
        auditTrail.append(AuditEventType.ALLOCATION_DRAWN, ActorRole.SYSTEM, null,
                "study:1", Map.of("commitment", "abc"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // The trigger blocks UPDATE, so tampering means inserting a forged row: the chain
        // verification must still catch it, because its hash cannot be right.
        Long lastSeq = jdbc.queryForObject("select max(seq) from audit.event", Long.class);
        String lastHash = jdbc.queryForObject(
                "select hash from audit.event where seq = ?", String.class, lastSeq);
        jdbc.update("""
                insert into audit.event
                    (occurred_at, event_type, actor_role, actor_code, subject_ref, payload, prev_hash, hash)
                values (now(), 'ANALYSIS_RUN', 'SYSTEM', null, 'study:1', '{"forged":true}', ?, ?)
                """, lastHash, "f".repeat(64));

        ChainVerification verification = auditTrail.verifyChain();
        assertThat(verification.intact()).isFalse();
        assertThat(verification.detail()).contains("alterado");
        // The test transaction rolls back, so the forged row does not survive into the next
        // test: the trail is append-only and a DELETE would be refused.
    }
}
