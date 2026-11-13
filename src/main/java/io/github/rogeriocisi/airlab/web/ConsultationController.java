package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.ConsultationDraft;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.OwnConsultation;
import io.github.rogeriocisi.airlab.structured.FeedbackService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.TargetFeedback;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sitter's P4 request (SPEC E13, phase B): fill in, review, lock, and see it afterwards.
 *
 * <p>The draft lives in the HTTP session until the sitter confirms; the database receives the
 * request once, already locked. Only the sitter's own request is ever shown here — the
 * principal's code is the only key used to look it up. Who reaches these pages at all (the
 * {@code SITTER} role) is decided in the security configuration.
 */
@Controller
class ConsultationController {

    static final String DRAFT = "p4.rascunho";
    private static final String ANSWER_PREFIX = "r.";

    private final ConsultationService consultations;
    private final FeedbackService feedback;
    private final StructuredStudyService studies;
    private final SignUpGate gate;

    ConsultationController(ConsultationService consultations, FeedbackService feedback,
                           StructuredStudyService studies, SignUpGate gate) {
        this.consultations = consultations;
        this.feedback = feedback;
        this.studies = studies;
        this.gate = gate;
    }

    @GetMapping("/painel/consulente/pedido/novo")
    String form(Principal principal, HttpSession session, Model model) {
        if (consultations.status(principal.getName()).isPresent()) {
            return "redirect:/painel/consulente/pedido";
        }
        if (!gate.requestsOpen()) {
            return "redirect:/painel/consulente";
        }
        return formPage(model, (ConsultationDraft) session.getAttribute(DRAFT));
    }

    @PostMapping("/painel/consulente/pedido/revisar")
    String review(Principal principal,
                  @RequestParam(name = "primeiroNome", defaultValue = "") String firstName,
                  @RequestParam Map<String, String> params,
                  HttpSession session,
                  Model model) {
        if (consultations.status(principal.getName()).isPresent()) {
            return "redirect:/painel/consulente/pedido";
        }
        ConsultationDraft draft = new ConsultationDraft(firstName, answersFrom(params));
        session.setAttribute(DRAFT, draft);
        try {
            consultations.check(draft);
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return formPage(model, draft);
        }
        model.addAttribute("questionario", Questionnaires.current());
        model.addAttribute("rascunho", draft);
        model.addAttribute("respostas", labelled(Questionnaires.current(), draft.answers()));
        return "pedido-revisar";
    }

    @PostMapping("/painel/consulente/pedido/confirmar")
    String confirm(Principal principal, HttpSession session) {
        ConsultationDraft draft = (ConsultationDraft) session.getAttribute(DRAFT);
        if (draft == null || !gate.requestsOpen()) {
            return "redirect:/painel/consulente/pedido/novo";
        }
        if (consultations.status(principal.getName()).isPresent()) {
            // A repeated submission: the request already exists, so show it.
            session.removeAttribute(DRAFT);
            return "redirect:/painel/consulente/pedido";
        }
        try {
            consultations.submit(principal.getName(), draft);
        } catch (IllegalArgumentException e) {
            // The draft in the session was never valid: back to the form, which shows why.
            return "redirect:/painel/consulente/pedido/novo";
        }
        session.removeAttribute(DRAFT);
        return "redirect:/painel/consulente/pedido?travado";
    }

    @GetMapping("/painel/consulente/pedido")
    String own(Principal principal, Model model) {
        Optional<OwnConsultation> own = consultations.own(principal.getName());
        if (own.isEmpty()) {
            return "redirect:/painel/consulente";
        }
        Questionnaire questionnaire = Questionnaires.byVersion(own.get().questionnaireVersion())
                .orElse(Questionnaires.current());
        model.addAttribute("pedido", own.get());
        model.addAttribute("situacao", consultations.status(principal.getName()).orElseThrow());
        model.addAttribute("respostas", labelled(questionnaire, own.get().answers()));
        return "pedido";
    }

    /**
     * The sitter's feedback (SPEC E13, phase D): their answers beside the medium's reading of their
     * own target, after the unblinding. The request is the principal's own; before the unblinding
     * there is no score to build from, and the page sends the sitter back to the panel.
     */
    @GetMapping("/painel/consulente/devolutiva")
    String feedback(Principal principal, Model model) {
        Optional<OwnConsultation> own = consultations.own(principal.getName());
        Optional<TargetFeedback> result = own.flatMap(request -> feedback.forRequest(request.code()));
        if (result.isEmpty()) {
            return "redirect:/painel/consulente";
        }
        Questionnaire questionnaire = Questionnaires.byVersion(result.get().questionnaireVersion())
                .orElse(Questionnaires.current());
        List<FeedbackRow> rows = new ArrayList<>();
        for (Questionnaire.Field field : questionnaire.scoredFields()) {
            String mine = own.get().answers().get(field.key());
            String read = result.get().readingAnswers().get(field.key());
            boolean comparable = result.get().comparableFields().contains(field.key());
            rows.add(new FeedbackRow(field.label(), questionnaire.labelOf(field.key(), mine),
                    read == null ? "—" : questionnaire.labelOf(field.key(), read), comparable,
                    comparable && read != null && read.equals(mine)));
        }
        model.addAttribute("pedido", own.get());
        model.addAttribute("devolutiva", result.get());
        model.addAttribute("linhas", rows);
        model.addAttribute("relatorio", studies.report(result.get().studyCode()).orElse(null));
        return "devolutiva";
    }

    /** One field of the feedback: the question, the two answers, and whether it counted and matched. */
    record FeedbackRow(String question, String mine, String reading, boolean comparable, boolean match) {
    }

    private String formPage(Model model, ConsultationDraft draft) {
        model.addAttribute("questionario", Questionnaires.current());
        model.addAttribute("rascunho", draft == null ? new ConsultationDraft("", Map.of()) : draft);
        model.addAttribute("nomeMaximo", ConsultationService.MAXIMUM_FIRST_NAME_LENGTH);
        model.addAttribute("naoSei", Questionnaire.UNKNOWN);
        return "pedido-novo";
    }

    private static Map<String, String> answersFrom(Map<String, String> params) {
        Map<String, String> answers = new LinkedHashMap<>();
        params.forEach((name, value) -> {
            if (name.startsWith(ANSWER_PREFIX)) {
                answers.put(name.substring(ANSWER_PREFIX.length()), value);
            }
        });
        return answers;
    }

    /** Question label to answer label, in questionnaire order, for showing a request back. */
    private static Map<String, String> labelled(Questionnaire questionnaire, Map<String, String> answers) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Questionnaire.Field field : questionnaire.fields()) {
            String answer = answers.get(field.key());
            if (answer != null) {
                out.put(field.label(), questionnaire.labelOf(field.key(), answer));
            }
        }
        return out;
    }
}
