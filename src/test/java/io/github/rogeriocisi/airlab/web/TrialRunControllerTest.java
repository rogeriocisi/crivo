package io.github.rogeriocisi.airlab.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.trials.SessionProgress;
import io.github.rogeriocisi.airlab.trials.SessionState;
import io.github.rogeriocisi.airlab.trials.TrialProtocol;
import io.github.rogeriocisi.airlab.trials.TrialSessionService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The pages of the self-service tests, from the outside.
 *
 * <p>Two things matter here. A repeated or stale submission must resume the test rather than
 * land the person on an error page or a login screen — reloading is something people do, and
 * the refusal already happened in the domain. And the catalog page must stay public: these
 * tests exist to be taken by anyone.
 *
 * <p>Runs with the real security configuration rather than a sliced web context, because
 * "this page is public" is a claim about that configuration. The service is mocked, so the
 * subject here is the pages and the flow, not the protocol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrialRunControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TrialSessionService trialSessions;

    @Test
    @DisplayName("o catalogo e a pagina do protocolo sao publicos")
    void the_catalog_is_public() throws Exception {
        mvc.perform(get("/testes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("catálogo")));
        mvc.perform(get("/testes/escolha-forcada")).andExpect(status().isOk());
        mvc.perform(get("/testes/visao-remota")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("protocolo que nao existe da 404, nao erro")
    void an_unknown_protocol_is_a_404() throws Exception {
        mvc.perform(get("/testes/telepatia-por-telefone")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem sessao aberta, a pagina de rodar mostra o convite")
    void without_a_session_the_run_page_invites() throws Exception {
        mvc.perform(get("/testes/escolha-forcada/rodar"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Começar a sessão")));
    }

    @Test
    @DisplayName("comecar a sessao leva para a tentativa")
    void starting_redirects_to_the_trial() throws Exception {
        given(trialSessions.start(any(), anyInt())).willReturn(progress());

        mvc.perform(post("/testes/escolha-forcada/comecar").param("tentativas", "12").with(csrf()))
                .andExpect(redirectedUrl("/testes/escolha-forcada/rodar"));
    }

    @Test
    @DisplayName("reenvio de resposta ja travada retoma o teste, sem erro e sem tela de login")
    void a_repeated_submission_resumes_the_test() throws Exception {
        given(trialSessions.start(any(), anyInt())).willReturn(progress());
        willThrow(new IllegalStateException("a sessao ja tem as tentativas declaradas"))
                .given(trialSessions).submitChoice(anyString(), anyInt());

        var session = mvc.perform(
                post("/testes/escolha-forcada/comecar").param("tentativas", "12").with(csrf()))
                .andReturn().getRequest().getSession();

        mvc.perform(post("/testes/escolha-forcada/escolha").param("posicao", "1")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testes/escolha-forcada/rodar"));
    }

    @Test
    @DisplayName("impressao reescrita depois das figuras retoma o teste, e nao e aceita")
    void rewriting_a_locked_impression_resumes_the_test() throws Exception {
        given(trialSessions.start(any(), anyInt()))
                .willReturn(progress(TrialProtocol.REMOTE_VIEWING_RANKED));
        willThrow(new IllegalStateException("ja foi travada"))
                .given(trialSessions).lockImpression(anyString(), anyString());

        var session = mvc.perform(
                post("/testes/visao-remota/comecar").param("tentativas", "3").with(csrf()))
                .andReturn().getRequest().getSession();

        mvc.perform(post("/testes/visao-remota/impressao").param("impressao", "outra coisa")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/testes/visao-remota/rodar"));
    }

    private static SessionProgress progress() {
        return progress(TrialProtocol.PRECOGNITIVE_FORCED_CHOICE);
    }

    private static SessionProgress progress(TrialProtocol protocol) {
        return new SessionProgress("TS-TEST0001", protocol, 12, 0, 1, "0".repeat(64),
                Instant.parse("2026-01-01T00:00:00Z"), SessionState.ACTIVE,
                protocol.impressionRequired());
    }
}
