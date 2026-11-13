package io.github.rogeriocisi.airlab.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.notification.MailMessage;
import io.github.rogeriocisi.airlab.notification.Mailer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The P4 account pages from the outside (SPEC E13, phase A), with the real security
 * configuration and the real database: sign up, confirm by the link that went to the outbox,
 * sign in, and land on the panel of one's own profile — and only on that one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountPagesTest {

    private static final String PASSWORD = "senha-de-teste-123";
    private static final Pattern TOKEN = Pattern.compile("token=([0-9A-Z]+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    Mailer mailer;

    private String signUp(String email, String profile) throws Exception {
        mvc.perform(post("/conta/criar").with(csrf())
                        .param("nome", "Pessoa de Teste")
                        .param("email", email)
                        .param("senha", PASSWORD)
                        .param("confirmacao", PASSWORD)
                        .param("perfil", profile)
                        .param("termo", "conta-v1")
                        .param("aceite", "true"))
                .andExpect(redirectedUrl("/conta/verifique-seu-email"));

        MailMessage message = mailer.outbox().stream()
                .filter(candidate -> candidate.to().equals(email))
                .findFirst().orElseThrow();
        Matcher matcher = TOKEN.matcher(message.body());
        assertThat(matcher.find()).as("o e-mail traz o link de confirmacao").isTrue();
        return matcher.group(1);
    }

    private static String uniqueEmail() {
        return "pagina-" + UUID.randomUUID() + "@exemplo.test";
    }

    @Test
    @DisplayName("criar conta, as paginas de protocolo e a inicial sao publicas")
    void the_public_pages_are_public() throws Exception {
        mvc.perform(get("/conta/criar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Consulente")))
                .andExpect(content().string(containsString("Médium")));
        mvc.perform(get("/testes")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Protocolos AIR")))
                .andExpect(content().string(containsString("Experimentos extras")));
        mvc.perform(get("/testes/leitura-estruturada")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Como participar, passo a passo")))
                .andExpect(content().string(containsString("O que cada perfil vê")));
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"air-automatizado\"")));
    }

    @Test
    @DisplayName("o AIR automatizado nao vira teste anonimo: nao ha rota de rodar sem conta")
    void the_automated_air_protocol_is_not_an_anonymous_test() throws Exception {
        mvc.perform(get("/testes/leitura-estruturada/rodar")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("do cadastro ao painel do consulente, passando pela confirmacao do e-mail")
    void from_sign_up_to_the_sitter_panel() throws Exception {
        String email = uniqueEmail();
        String token = signUp(email, "consulente");

        // Before confirming: the right password says what is missing, a wrong one says nothing.
        mvc.perform(formLogin("/login").user(email).password(PASSWORD))
                .andExpect(redirectedUrl("/login?naoConfirmada"));
        mvc.perform(formLogin("/login").user(email).password("senha-errada-123"))
                .andExpect(redirectedUrl("/login?error"));

        mvc.perform(get("/conta/confirmar").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar meu e-mail")));
        mvc.perform(post("/conta/confirmar").with(csrf()).param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conta confirmada")));

        MvcResult login = mvc.perform(formLogin("/login").user(email).password(PASSWORD))
                .andExpect(redirectedUrl("/painel"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/painel").session(session)).andExpect(redirectedUrl("/painel/consulente"));
        mvc.perform(get("/painel/consulente").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Olá, Pessoa")));
        mvc.perform(get("/conta").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Consulente (fixo)")));

        // Only the panel of one's own profile.
        mvc.perform(get("/painel/medium").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/painel/adm").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/caixa-de-saida").session(session)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o medium cai no painel dele e nao alcanca o do consulente")
    void the_medium_reaches_only_the_medium_panel() throws Exception {
        String email = uniqueEmail();
        String token = signUp(email, "medium");
        mvc.perform(post("/conta/confirmar").with(csrf()).param("token", token)).andExpect(status().isOk());

        MvcResult login = mvc.perform(formLogin("/login").user(email).password(PASSWORD)).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/painel").session(session)).andExpect(redirectedUrl("/painel/medium"));
        mvc.perform(get("/painel/medium").session(session)).andExpect(status().isOk());
        mvc.perform(get("/painel/consulente").session(session)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o cadastro repetido mostra a mesma pagina e nao revela quem tem conta")
    void a_repeated_sign_up_reveals_nothing() throws Exception {
        String email = uniqueEmail();
        signUp(email, "consulente");
        mvc.perform(post("/conta/criar").with(csrf())
                        .param("nome", "Outra Pessoa")
                        .param("email", email)
                        .param("senha", PASSWORD)
                        .param("confirmacao", PASSWORD)
                        .param("perfil", "medium")
                        .param("termo", "conta-v1")
                        .param("aceite", "true"))
                .andExpect(redirectedUrl("/conta/verifique-seu-email"));
    }

    @Test
    @DisplayName("erro de formulario volta para a pagina, sem devolver a senha")
    void a_form_error_returns_to_the_page() throws Exception {
        mvc.perform(post("/conta/criar").with(csrf())
                        .param("nome", "Pessoa")
                        .param("email", uniqueEmail())
                        .param("senha", PASSWORD)
                        .param("confirmacao", "outra-senha-qualquer")
                        .param("perfil", "consulente")
                        .param("termo", "conta-v1")
                        .param("aceite", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A confirmação não é igual à senha.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(PASSWORD))));
    }

    @Test
    @DisplayName("o administrador ve contagens, avisos e a caixa de saida local")
    void the_administrator_sees_counts_and_notices() throws Exception {
        mvc.perform(get("/painel/adm").with(user("pesquisador").roles("ADMIN", "RESEARCHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Consulentes confirmados")))
                .andExpect(content().string(containsString("nunca notas")));
        mvc.perform(get("/caixa-de-saida").with(user("pesquisador").roles("ADMIN", "RESEARCHER")))
                .andExpect(status().isOk());
        mvc.perform(get("/painel").with(user("pesquisador").roles("ADMIN", "RESEARCHER")))
                .andExpect(redirectedUrl("/painel/adm"));
    }

    @Test
    @DisplayName("sem login, os paineis mandam para a entrada")
    void panels_require_sign_in() throws Exception {
        mvc.perform(get("/painel/consulente")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/painel/adm")).andExpect(status().is3xxRedirection());
    }
}
