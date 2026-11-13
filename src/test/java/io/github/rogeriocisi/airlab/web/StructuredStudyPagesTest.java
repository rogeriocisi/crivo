package io.github.rogeriocisi.airlab.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyTerms;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The administrator's P4 preregistration pages (SPEC E13, phase C): fill in, review with the
 * declared power, freeze.
 *
 * <p>The study service is a mock here on purpose. The shared test database keeps its one open P4
 * study across runs, so with the real service the form would appear only on a fresh database;
 * the freezing itself is covered against the real database by {@code StructuredStudyIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructuredStudyPagesTest {

    private static final RequestPostProcessor ADMIN = user("pesquisador").roles("ADMIN", "RESEARCHER");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    StructuredStudyService studies;

    @BeforeEach
    void noStudyYet() {
        when(studies.current()).thenReturn(Optional.empty());
        when(studies.draftQuestionnaireAllowed()).thenReturn(true);
    }

    private static MockHttpServletRequestBuilder filled(MockHttpSession session) {
        return post("/painel/adm/estudo-p4/revisar").with(ADMIN).with(csrf()).session(session)
                .param("titulo", "Estudo de telas")
                .param("pares", "40")
                .param("prazo", "10")
                .param("efeito", "0.5")
                .param("hipotese", "As leituras coincidem mais com o alvo.")
                .param("criterios", "Qualquer medium confirmado.");
    }

    @Test
    @DisplayName("preencher, rever com o poder declarado e congelar")
    void fill_review_and_freeze() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/painel/adm/estudo-p4").with(ADMIN).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pré-registro do estudo")))
                .andExpect(content().string(containsString("Rever antes de congelar")));

        mvc.perform(filled(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Congelar o pré-registro")))
                .andExpect(content().string(containsString("Tamanho-alvo de 40")))
                .andExpect(content().string(containsString("Estudo de telas")));

        mvc.perform(post("/painel/adm/estudo-p4/congelar").with(ADMIN).with(csrf()).session(session))
                .andExpect(redirectedUrl("/painel/adm/estudo-p4?congelado"));
        verify(studies).open(argThat((StructuredStudyTerms terms) -> terms.targetPairs() == 40
                && terms.deadlineDays() == 10 && terms.title().equals("Estudo de telas")), eq("pesquisador"));
    }

    @Test
    @DisplayName("termos invalidos voltam ao formulario com a mensagem")
    void invalid_terms_return_with_the_message() throws Exception {
        doThrow(new IllegalArgumentException("O número de pares vai de 1 a 10000.")).when(studies).check(any());
        mvc.perform(filled(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O número de pares vai de 1 a 10000.")))
                .andExpect(content().string(not(containsString("Congelar o pré-registro"))));
    }

    @Test
    @DisplayName("questionario em rascunho, onde rascunho nao vale, nao oferece o congelamento")
    void a_draft_questionnaire_cannot_be_frozen_where_drafts_are_not_allowed() throws Exception {
        when(studies.draftQuestionnaireAllowed()).thenReturn(false);
        mvc.perform(filled(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Não é possível congelar aqui")))
                .andExpect(content().string(not(containsString("Congelar o pré-registro"))));
    }

    @Test
    @DisplayName("so o administrador alcanca o estudo")
    void only_the_administrator_reaches_the_study() throws Exception {
        mvc.perform(get("/painel/adm/estudo-p4").with(user("MED-X").roles("MEDIUM")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/painel/adm/estudo-p4").with(user("CON-X").roles("SITTER")))
                .andExpect(status().isForbidden());
    }
}
