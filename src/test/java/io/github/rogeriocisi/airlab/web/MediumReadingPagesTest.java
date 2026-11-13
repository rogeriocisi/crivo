package io.github.rogeriocisi.airlab.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.structured.P4Fixture;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The medium's P4 pages (SPEC E13, phase C) with the real security configuration and database:
 * see the request, answer, review, lock — and check that nobody else sees the names.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediumReadingPagesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountService accounts;

    @Autowired
    ConsultationService consultations;

    @Autowired
    StructuredStudyService studies;

    @Autowired
    JdbcTemplate jdbc;

    P4Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new P4Fixture(accounts, consultations, studies, jdbc);
    }

    private static RequestPostProcessor medium(String code) {
        return user(code).roles("MEDIUM");
    }

    private static MockHttpServletRequestBuilder filledReading(String assignment, int position) {
        MockHttpServletRequestBuilder request = post("/painel/medium/pedido/{a}/leitura/{p}/revisar", assignment, position)
                .with(csrf());
        boolean first = true;
        for (Questionnaire.Field field : Questionnaires.current().scoredFields()) {
            // The first field says "nothing came", the others pick an option.
            request.param("r." + field.key(), first ? Questionnaire.NOT_RECEIVED : field.options().get(0).key());
            first = false;
        }
        return request;
    }

    @Test
    @DisplayName("do painel a leitura travada: ver os nomes, responder, rever e travar")
    void from_panel_to_locked_reading() throws Exception {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("feminino", "40-59", "5-15-anos"),
                "Benedita", "Conceicao");
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/painel/medium").with(medium(pair.medium())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Benedita")))
                .andExpect(content().string(containsString("Conceicao")))
                .andExpect(content().string(containsString(pair.assignment())))
                .andExpect(content().string(containsString("Responder")));

        mvc.perform(get("/painel/medium/pedido/{a}/leitura/1", pair.assignment()).with(medium(pair.medium())).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Não recebi")))
                .andExpect(content().string(containsString("Causa principal")))
                .andExpect(content().string(not(containsString("Gênero"))));

        mvc.perform(filledReading(pair.assignment(), 1).with(medium(pair.medium())).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Travar a leitura")))
                .andExpect(content().string(containsString("Não recebi")));

        mvc.perform(post("/painel/medium/pedido/{a}/leitura/1/travar", pair.assignment())
                        .with(medium(pair.medium())).with(csrf()).session(session))
                .andExpect(redirectedUrl("/painel/medium?travada"));

        mvc.perform(get("/painel/medium").with(medium(pair.medium())))
                .andExpect(content().string(containsString("Leitura travada")));
        // A locked position is not offered again.
        mvc.perform(get("/painel/medium/pedido/{a}/leitura/1", pair.assignment()).with(medium(pair.medium())))
                .andExpect(redirectedUrl("/painel/medium"));
    }

    @Test
    @DisplayName("uma leitura incompleta volta com a mensagem e nada e travado")
    void an_incomplete_reading_returns_with_the_message() throws Exception {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("feminino", "60-79", "5-15-anos"),
                "Iracema", "Jandira");
        mvc.perform(post("/painel/medium/pedido/{a}/leitura/2/revisar", pair.assignment())
                        .with(medium(pair.medium())).with(csrf())
                        .param("r.causa", Questionnaire.NOT_RECEIVED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Responda ao campo")));
        mvc.perform(get("/painel/medium/pedido/{a}/leitura/2", pair.assignment()).with(medium(pair.medium())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("os nomes nao aparecem para outro medium, para o consulente nem para o administrador")
    void nobody_else_sees_the_names() throws Exception {
        P4Fixture.Pair pair = fixture.pairOf(new P4Fixture.Stratum("feminino", "18-39", "5-15-anos"),
                "Florisbela", "Genoveva");
        String other = fixture.account(AccountProfile.MEDIUM);

        mvc.perform(get("/painel/medium").with(medium(other)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Florisbela"))));
        mvc.perform(get("/painel/medium/pedido/{a}/leitura/1", pair.assignment()).with(medium(other)))
                .andExpect(redirectedUrl("/painel/medium"));
        mvc.perform(filledReading(pair.assignment(), 1).with(medium(other)))
                .andExpect(redirectedUrl("/painel/medium"));

        mvc.perform(get("/painel/medium/pedido/{a}/leitura/1", pair.assignment())
                        .with(user(pair.a().sitter()).roles("SITTER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/painel/consulente").with(user(pair.a().sitter()).roles("SITTER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Com o médium")))
                .andExpect(content().string(not(containsString(pair.medium()))));

        RequestPostProcessor admin = user("pesquisador").roles("ADMIN", "RESEARCHER");
        mvc.perform(get("/painel/adm").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pares ativos")))
                .andExpect(content().string(not(containsString("Florisbela"))))
                .andExpect(content().string(not(containsString(pair.medium()))));
        mvc.perform(get("/painel/adm/estudo-p4").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O que foi congelado")))
                .andExpect(content().string(containsString("Compromisso da semente")))
                .andExpect(content().string(not(containsString("Florisbela"))))
                .andExpect(content().string(not(containsString(pair.medium()))))
                .andExpect(content().string(not(containsString(pair.assignment()))));
    }
}
