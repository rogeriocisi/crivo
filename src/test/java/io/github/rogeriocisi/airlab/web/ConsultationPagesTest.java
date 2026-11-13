package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.TestNames;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.AccountProfile;
import io.github.rogeriocisi.airlab.participant.AccountRegistration;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountSignUp;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The sitter's P4 request pages (SPEC E13, phase B), with the real security configuration and
 * database: fill in, review, lock, see it — and check that nobody else sees it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultationPagesTest {

    private static final String PASSWORD = "senha-de-teste-123";
    private static final String FIRST_NAME = "Benedita";

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountService accounts;

    private MockHttpSession signedIn(AccountProfile profile) throws Exception {
        String email = "telas-" + UUID.randomUUID() + "@exemplo.test";
        AccountRegistration registration = accounts.register(new AccountSignUp(TestNames.unique("Pessoa"), email, PASSWORD,
                profile, AccountService.CURRENT_TERM_VERSION, true));
        accounts.confirmEmail(registration.confirmationToken());
        return (MockHttpSession) mvc.perform(formLogin("/login").user(email).password(PASSWORD))
                .andReturn().getRequest().getSession(false);
    }

    private static MockHttpServletRequestBuilder filledForm(MockHttpSession session) {
        MockHttpServletRequestBuilder request = post("/painel/consulente/pedido/revisar").session(session).with(csrf())
                .param("primeiroNome", FIRST_NAME);
        for (Questionnaire.Field field : Questionnaires.current().fields()) {
            request.param("r." + field.key(), field.options().get(0).key());
        }
        return request;
    }

    @Test
    @DisplayName("do painel ao pedido travado: preencher, rever, confirmar e ver")
    void from_panel_to_locked_request() throws Exception {
        MockHttpSession session = signedIn(AccountProfile.SITTER);

        mvc.perform(get("/painel/consulente").session(session))
                .andExpect(content().string(containsString("Fazer o pedido")));
        mvc.perform(get("/painel/consulente/pedido/novo").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Primeiro nome da pessoa que faleceu")))
                .andExpect(content().string(containsString("Causa principal")))
                .andExpect(content().string(containsString("Não sei")));

        mvc.perform(filledForm(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(FIRST_NAME)))
                .andExpect(content().string(containsString("Confirmar e travar")));

        mvc.perform(post("/painel/consulente/pedido/confirmar").session(session).with(csrf()))
                .andExpect(redirectedUrl("/painel/consulente/pedido?travado"));

        mvc.perform(get("/painel/consulente/pedido").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(FIRST_NAME)))
                .andExpect(content().string(containsString("Compromisso publicado")))
                .andExpect(content().string(containsString("Travado")));
        mvc.perform(get("/painel/consulente").session(session))
                .andExpect(content().string(containsString("Ver meu pedido")));

        // A second request is not offered, and a repeated confirmation shows the existing one.
        mvc.perform(get("/painel/consulente/pedido/novo").session(session))
                .andExpect(redirectedUrl("/painel/consulente/pedido"));
        mvc.perform(post("/painel/consulente/pedido/confirmar").session(session).with(csrf()))
                .andExpect(redirectedUrl("/painel/consulente/pedido/novo"));
    }

    @Test
    @DisplayName("formulario incompleto volta com a mensagem e nada e travado")
    void an_incomplete_form_returns_with_the_message() throws Exception {
        MockHttpSession session = signedIn(AccountProfile.SITTER);

        mvc.perform(post("/painel/consulente/pedido/revisar").session(session).with(csrf())
                        .param("primeiroNome", FIRST_NAME)
                        .param("r.causa", "cancer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Responda ao campo")));
        mvc.perform(get("/painel/consulente/pedido").session(session))
                .andExpect(redirectedUrl("/painel/consulente"));
    }

    @Test
    @DisplayName("o pedido de um consulente nao aparece para outro, nem para o administrador")
    void nobody_else_sees_the_request() throws Exception {
        MockHttpSession owner = signedIn(AccountProfile.SITTER);
        mvc.perform(filledForm(owner));
        mvc.perform(post("/painel/consulente/pedido/confirmar").session(owner).with(csrf()));

        MockHttpSession other = signedIn(AccountProfile.SITTER);
        mvc.perform(get("/painel/consulente/pedido").session(other))
                .andExpect(redirectedUrl("/painel/consulente"));
        mvc.perform(get("/painel/consulente").session(other))
                .andExpect(content().string(not(containsString(FIRST_NAME))));

        mvc.perform(get("/painel/adm").with(user("pesquisador").roles("ADMIN", "RESEARCHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedidos travados")))
                .andExpect(content().string(not(containsString(FIRST_NAME))));
    }

    @Test
    @DisplayName("o medium nao alcanca as telas de pedido")
    void the_medium_cannot_reach_request_pages() throws Exception {
        MockHttpSession medium = signedIn(AccountProfile.MEDIUM);
        mvc.perform(get("/painel/consulente/pedido/novo").session(medium)).andExpect(status().isForbidden());
        mvc.perform(get("/painel/consulente/pedido").session(medium)).andExpect(status().isForbidden());
        mvc.perform(filledForm(medium)).andExpect(status().isForbidden());
    }
}
