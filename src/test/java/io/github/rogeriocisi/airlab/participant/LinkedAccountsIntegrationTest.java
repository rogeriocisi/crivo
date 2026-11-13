package io.github.rogeriocisi.airlab.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.TestNames;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Linked accounts (SPEC E13, phase E) against the real PostgreSQL: a shared signal flags, never
 * blocks; the trail never names the accounts; a decision is made once; withdrawal erases the
 * signals.
 */
@SpringBootTest
@ActiveProfiles("test")
class LinkedAccountsIntegrationTest {

    @Autowired
    AccountService accounts;

    @Autowired
    AdminNotices notices;

    @Autowired
    JdbcTemplate jdbc;

    private String confirmed(AccountProfile profile, String fullName, String email) {
        AccountRegistration registration = accounts.register(new AccountSignUp(fullName, email,
                "senha-de-teste-123", profile, AccountService.CURRENT_TERM_VERSION, true));
        accounts.confirmEmail(registration.confirmationToken());
        return registration.code();
    }

    private static String email() {
        return "ligada-" + UUID.randomUUID() + "@exemplo.test";
    }

    private AccountFlag flagBetween(String one, String other, LinkSignal signal) {
        return accounts.flags().stream()
                .filter(flag -> flag.signal() == signal)
                .filter(flag -> List.of(flag.accountA(), flag.accountB()).containsAll(List.of(one, other)))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("mesmo nome completo em consulente e medium: sinaliza, nao bloqueia, e a trilha nao nomeia as contas")
    void the_same_full_name_flags_without_blocking() {
        String name = TestNames.unique("Linda");
        String sitter = confirmed(AccountProfile.SITTER, name, email());
        String medium = confirmed(AccountProfile.MEDIUM, name.toUpperCase(), email());

        AccountFlag flag = flagBetween(sitter, medium, LinkSignal.FULL_NAME);
        assertThat(flag.pending()).isTrue();
        assertThat(flag.keepsApart()).isTrue();
        // Flagged, not blocked: both accounts stay usable.
        assertThat(accounts.account(sitter)).isPresent();
        assertThat(accounts.activeMediumCodes()).contains(medium);
        assertThat(accounts.keptApart(List.of(sitter)).get(sitter)).containsExactly(medium);
        assertThat(notices.recent(50)).anyMatch(notice -> notice.kind() == NoticeKind.ACCOUNTS_FLAGGED
                && notice.subjectCode().equals(flag.code()));

        String trail = jdbc.queryForObject("SELECT actor_role || ' ' || coalesce(actor_code, '-') || ' ' || payload "
                + "FROM audit.event WHERE event_type = 'ACCOUNT_LINK_FLAGGED' AND subject_ref = ?",
                String.class, "flag:" + flag.code());
        assertThat(trail).contains("FULL_NAME").doesNotContain(sitter).doesNotContain(medium);
    }

    @Test
    @DisplayName("a decisao vale uma vez: sem relacao, a restricao cai; o banco recusa reescrever ou apagar")
    void a_decision_is_made_once() {
        String name = TestNames.unique("Iara");
        String sitter = confirmed(AccountProfile.SITTER, name, email());
        String medium = confirmed(AccountProfile.MEDIUM, name, email());
        AccountFlag flag = flagBetween(sitter, medium, LinkSignal.FULL_NAME);

        accounts.reviewFlag(flag.code(), FlagDecision.UNRELATED, "pesquisador");

        assertThat(accounts.keptApart(List.of(sitter)).get(sitter)).isEmpty();
        assertThat(flagBetween(sitter, medium, LinkSignal.FULL_NAME).decision()).isEqualTo(FlagDecision.UNRELATED);
        assertThatThrownBy(() -> accounts.reviewFlag(flag.code(), FlagDecision.LINKED, "pesquisador"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT payload FROM audit.event WHERE event_type = 'ACCOUNT_LINK_REVIEWED' "
                + "AND subject_ref = ?", String.class, "flag:" + flag.code()))
                .contains("UNRELATED").doesNotContain(sitter).doesNotContain(medium);

        assertThatThrownBy(() -> jdbc.update("UPDATE identity.account_flag SET decision = 'LINKED' WHERE code = ?",
                flag.code())).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM identity.account_flag WHERE code = ?", flag.code()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("o mesmo e-mail do Gmail com pontos e apelido e sinalizado; uma palavra so de nome nao vira sinal")
    void gmail_aliases_are_the_same_mailbox() {
        String box = "crivo" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String one = confirmed(AccountProfile.SITTER, TestNames.unique("Rosa"), box.substring(0, 4) + "." + box.substring(4) + "@gmail.com");
        String other = confirmed(AccountProfile.MEDIUM, TestNames.unique("Lia"), box + "+medium@googlemail.com");

        assertThat(flagBetween(one, other, LinkSignal.EMAIL_CANONICAL).pending()).isTrue();

        String single = confirmed(AccountProfile.SITTER, "Zebedeu", email());
        assertThat(jdbc.queryForList("SELECT kind FROM identity.account_signal WHERE account_code = ?",
                String.class, single)).containsExactly("EMAIL_CANONICAL");
    }

    @Test
    @DisplayName("a desistencia apaga os sinais da conta; a sinalizacao fica, so com os codigos")
    void withdrawal_erases_the_signals() {
        String name = TestNames.unique("Nara");
        String sitter = confirmed(AccountProfile.SITTER, name, email());
        String medium = confirmed(AccountProfile.MEDIUM, name, email());
        AccountFlag flag = flagBetween(sitter, medium, LinkSignal.FULL_NAME);

        accounts.close(sitter);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM identity.account_signal WHERE account_code = ?",
                Long.class, sitter)).isZero();
        assertThat(accounts.flags()).extracting(AccountFlag::code).contains(flag.code());
        // A new account with the same name no longer meets the closed one.
        String later = confirmed(AccountProfile.SITTER, name, email());
        assertThat(accounts.keptApart(List.of(later)).get(later)).containsExactly(medium).doesNotContain(sitter);
    }
}
