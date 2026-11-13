package io.github.rogeriocisi.airlab.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rogeriocisi.airlab.notification.AdminNotice;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * P4 accounts (SPEC E13, phase A), against the real PostgreSQL: the fixed profile and the
 * "no account is ever deleted" rule live in a trigger, and a trigger is only tested by a
 * database that runs it.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountIntegrationTest {

    private static final String PASSWORD = "senha-de-teste-123";

    @Autowired
    AccountService accounts;

    @Autowired
    AdminNotices notices;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbc;

    private static String uniqueEmail() {
        return "conta-" + UUID.randomUUID() + "@exemplo.test";
    }

    private static AccountSignUp signUp(String email, AccountProfile profile) {
        return new AccountSignUp("Maria Teste da Silva", email, PASSWORD, profile,
                AccountService.CURRENT_TERM_VERSION, true);
    }

    @Test
    @DisplayName("a conta nasce sem confirmacao e so vale depois do link")
    void an_account_is_valid_only_after_confirmation() {
        String email = uniqueEmail();
        AccountRegistration registration = accounts.register(signUp(email, AccountProfile.SITTER));

        assertThat(registration.code()).startsWith("CON-");
        assertThat(registration.alreadyExisted()).isFalse();
        assertThat(registration.confirmationToken()).isNotBlank();

        AccountCredentials before = accounts.credentials(email).orElseThrow();
        assertThat(before.confirmed()).isFalse();
        assertThat(before.code()).isEqualTo(registration.code());
        assertThat(passwordEncoder.matches(PASSWORD, before.passwordHash())).isTrue();

        assertThat(accounts.confirmEmail(registration.confirmationToken()))
                .isEqualTo(EmailConfirmationResult.CONFIRMED);
        assertThat(accounts.credentials(email).orElseThrow().confirmed()).isTrue();
        assertThat(accounts.confirmEmail(registration.confirmationToken()))
                .isEqualTo(EmailConfirmationResult.ALREADY_USED);
        assertThat(accounts.confirmEmail("nao-existe")).isEqualTo(EmailConfirmationResult.UNKNOWN);
    }

    @Test
    @DisplayName("a confirmacao avisa o administrador, so com tipo, codigo e hora")
    void confirmation_notifies_the_administrator() {
        AccountRegistration registration = accounts.register(signUp(uniqueEmail(), AccountProfile.MEDIUM));
        accounts.confirmEmail(registration.confirmationToken());

        List<AdminNotice> recent = notices.recent(20);
        assertThat(recent).anySatisfy(notice -> {
            assertThat(notice.kind()).isEqualTo(NoticeKind.ACCOUNT_CONFIRMED);
            assertThat(notice.subjectCode()).isEqualTo(registration.code());
        });
    }

    @Test
    @DisplayName("link vencido nao confirma")
    void an_expired_link_does_not_confirm() {
        String email = uniqueEmail();
        AccountRegistration registration = accounts.register(signUp(email, AccountProfile.SITTER));
        jdbc.update("UPDATE identity.email_confirmation SET created_at = now() - interval '3 days', "
                + "expires_at = now() - interval '1 day' WHERE account_code = ?", registration.code());

        assertThat(accounts.confirmEmail(registration.confirmationToken()))
                .isEqualTo(EmailConfirmationResult.EXPIRED);
        assertThat(accounts.credentials(email).orElseThrow().confirmed()).isFalse();
    }

    @Test
    @DisplayName("o mesmo e-mail nao cria segunda conta, nem com outro perfil e outra caixa")
    void the_same_email_never_creates_a_second_account() {
        String email = uniqueEmail();
        AccountRegistration first = accounts.register(signUp(email, AccountProfile.SITTER));

        AccountRegistration again = accounts.register(signUp(email.toUpperCase(), AccountProfile.MEDIUM));
        assertThat(again.alreadyExisted()).isTrue();
        assertThat(again.code()).isEqualTo(first.code());
        assertThat(again.profile()).isEqualTo(AccountProfile.SITTER);
        // Not yet confirmed: a fresh link, for the account and the profile that already exist.
        assertThat(again.confirmationToken()).isNotBlank().isNotEqualTo(first.confirmationToken());

        accounts.confirmEmail(again.confirmationToken());
        AccountRegistration third = accounts.register(signUp(email, AccountProfile.MEDIUM));
        assertThat(third.alreadyExisted()).isTrue();
        assertThat(third.confirmationToken()).isNull();
    }

    @Test
    @DisplayName("o perfil e fixo: o banco recusa a troca, mesmo por SQL direto")
    void the_profile_is_fixed_in_the_database() {
        AccountRegistration registration = accounts.register(signUp(uniqueEmail(), AccountProfile.SITTER));

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE identity.account SET profile = 'MEDIUM' WHERE code = ?", registration.code()))
                .hasMessageContaining("perfil e fixo");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE identity.account SET profile = 'ADMIN' WHERE code = ?", registration.code()))
                .isNotNull();
    }

    @Test
    @DisplayName("nenhuma conta e apagada: o banco recusa DELETE")
    void accounts_are_never_deleted() {
        AccountRegistration registration = accounts.register(signUp(uniqueEmail(), AccountProfile.MEDIUM));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM identity.account WHERE code = ?", registration.code()))
                .hasMessageContaining("nao pode ser apagada");
    }

    @Test
    @DisplayName("nao existe caminho para criar administrador")
    void there_is_no_administrator_profile() {
        assertThat(AccountProfile.values()).extracting(Enum::name).containsExactlyInAnyOrder("SITTER", "MEDIUM");
        assertThatThrownBy(() -> accounts.register(signUp(uniqueEmail(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("senha curta, termo antigo ou sem aceite: recusado com mensagem para a pessoa")
    void invalid_sign_ups_are_refused() {
        assertThatThrownBy(() -> accounts.register(new AccountSignUp("Ana", uniqueEmail(), "curta",
                AccountProfile.SITTER, AccountService.CURRENT_TERM_VERSION, true)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("senha");
        assertThatThrownBy(() -> accounts.register(new AccountSignUp("Ana", uniqueEmail(), PASSWORD,
                AccountProfile.SITTER, "conta-v0", true)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("termo");
        assertThatThrownBy(() -> accounts.register(new AccountSignUp("Ana", uniqueEmail(), PASSWORD,
                AccountProfile.SITTER, AccountService.CURRENT_TERM_VERSION, false)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("aceitar");
        assertThatThrownBy(() -> accounts.register(new AccountSignUp("Ana", "sem-arroba", PASSWORD,
                AccountProfile.SITTER, AccountService.CURRENT_TERM_VERSION, true)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("nome e e-mail nunca vao para a trilha publica nem ficam em claro no banco")
    void personal_data_stays_out_of_the_trail_and_encrypted() {
        String email = uniqueEmail();
        AccountRegistration registration = accounts.register(signUp(email, AccountProfile.SITTER));
        accounts.confirmEmail(registration.confirmationToken());

        List<String> payloads = jdbc.queryForList(
                "SELECT payload FROM audit.event WHERE subject_ref = ?", String.class,
                "account:" + registration.code());
        assertThat(payloads).hasSizeGreaterThanOrEqualTo(2);
        assertThat(payloads).noneMatch(payload -> payload.contains(email) || payload.contains("Maria"));

        String stored = jdbc.queryForObject(
                "SELECT full_name_enc || coalesce(email_enc, '') FROM identity.account WHERE code = ?",
                String.class, registration.code());
        assertThat(stored).doesNotContain("Maria").doesNotContain(email);
    }

    @Test
    @DisplayName("excluir a conta apaga os dados, mantem o codigo e libera o e-mail")
    void closing_erases_and_keeps_the_code() {
        String email = uniqueEmail();
        AccountRegistration registration = accounts.register(signUp(email, AccountProfile.SITTER));
        accounts.confirmEmail(registration.confirmationToken());

        accounts.close(registration.code());

        assertThat(accounts.credentials(email)).isEmpty();
        assertThat(accounts.account(registration.code())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM identity.account WHERE code = ? AND closed_at IS NOT NULL",
                Integer.class, registration.code())).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE identity.account SET term_version = 'x' WHERE code = ?", registration.code()))
                .hasMessageContaining("encerrada");

        AccountRegistration fresh = accounts.register(signUp(email, AccountProfile.MEDIUM));
        assertThat(fresh.alreadyExisted()).isFalse();
        assertThat(fresh.code()).isNotEqualTo(registration.code()).startsWith("MED-");
    }
}
