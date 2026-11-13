package io.github.rogeriocisi.airlab.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.TestNames;
import io.github.rogeriocisi.airlab.participant.AccountFlag;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountRegistration;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountSignUp;
import io.github.rogeriocisi.airlab.participant.FlagDecision;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The administrator's review of linked-accounts flags (SPEC E13, phase E). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinkedAccountsPagesTest {

    private static final RequestPostProcessor ADMIN = user("pesquisador").roles("ADMIN", "RESEARCHER");

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountService accounts;

    private String confirmed(AccountProfile profile, String fullName) {
        AccountRegistration registration = accounts.register(new AccountSignUp(fullName,
                "pagina-" + UUID.randomUUID() + "@exemplo.test", "senha-de-teste-123", profile,
                AccountService.CURRENT_TERM_VERSION, true));
        accounts.confirmEmail(registration.confirmationToken());
        return registration.code();
    }

    @Test
    @DisplayName("o administrador ve a sinalizacao com codigos e sinal, sem nome, e decide uma vez")
    void the_administrator_reviews_a_flag() throws Exception {
        String name = TestNames.unique("Odete");
        String sitter = confirmed(AccountProfile.SITTER, name);
        String medium = confirmed(AccountProfile.MEDIUM, name);
        AccountFlag flag = accounts.flags().stream()
                .filter(candidate -> List.of(candidate.accountA(), candidate.accountB()).containsAll(List.of(sitter, medium)))
                .findFirst().orElseThrow();

        mvc.perform(get("/painel/adm").with(ADMIN))
                .andExpect(content().string(containsString("Sinalizações")));
        mvc.perform(get("/painel/adm/sinalizacoes").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(flag.code())))
                .andExpect(content().string(containsString(sitter)))
                .andExpect(content().string(containsString("Mesmo nome completo")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(name))));

        mvc.perform(post("/painel/adm/sinalizacoes/{code}", flag.code()).with(ADMIN).with(csrf())
                        .param("decisao", FlagDecision.LINKED.name()))
                .andExpect(redirectedUrl("/painel/adm/sinalizacoes?revisada"));
        mvc.perform(post("/painel/adm/sinalizacoes/{code}", flag.code()).with(ADMIN).with(csrf())
                        .param("decisao", FlagDecision.UNRELATED.name()))
                .andExpect(redirectedUrl("/painel/adm/sinalizacoes"));
        org.assertj.core.api.Assertions.assertThat(accounts.flags().stream()
                .filter(candidate -> candidate.code().equals(flag.code())).findFirst().orElseThrow().decision())
                .isEqualTo(FlagDecision.LINKED);
    }

    @Test
    @DisplayName("so o administrador alcanca as sinalizacoes")
    void only_the_administrator_reaches_the_flags() throws Exception {
        mvc.perform(get("/painel/adm/sinalizacoes").with(user("MED-X").roles("MEDIUM")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/painel/adm/sinalizacoes").with(user("CON-X").roles("SITTER")))
                .andExpect(status().isForbidden());
    }
}
