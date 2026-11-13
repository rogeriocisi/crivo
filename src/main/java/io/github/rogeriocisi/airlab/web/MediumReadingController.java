package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.structured.MediumAssignment;
import io.github.rogeriocisi.airlab.structured.MediumReadingDraft;
import io.github.rogeriocisi.airlab.structured.MediumReadingService;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The medium's reading (SPEC E13, phase C): one form per name of the pair, a review, and the lock.
 *
 * <p>Every lookup goes through the principal's own code, so a medium who types another request's
 * code in the address bar gets the panel back, not the names. The draft lives in the HTTP session;
 * the database receives the reading once, already locked. Who reaches these pages at all (the
 * {@code MEDIUM} role) is decided in the security configuration.
 */
@Controller
class MediumReadingController {

    static final String DRAFT = "p4.leitura";
    private static final String ANSWER_PREFIX = "r.";
    private static final String PANEL = "redirect:/painel/medium";

    private final MediumReadingService readings;

    MediumReadingController(MediumReadingService readings) {
        this.readings = readings;
    }

    @GetMapping("/painel/medium/pedido/{code}/leitura/{position}")
    String form(Principal principal, @PathVariable String code, @PathVariable int position,
                HttpSession session, Model model) {
        Optional<MediumAssignment> assignment = writable(principal, code, position);
        if (assignment.isEmpty()) {
            return PANEL;
        }
        MediumReadingDraft draft = (MediumReadingDraft) session.getAttribute(DRAFT);
        if (draft == null || !draft.assignmentCode().equals(code) || draft.position() != position) {
            draft = new MediumReadingDraft(code, position, Map.of());
        }
        return formPage(model, assignment.get(), position, draft);
    }

    @PostMapping("/painel/medium/pedido/{code}/leitura/{position}/revisar")
    String review(Principal principal, @PathVariable String code, @PathVariable int position,
                  @RequestParam Map<String, String> params, HttpSession session, Model model) {
        Optional<MediumAssignment> assignment = writable(principal, code, position);
        if (assignment.isEmpty()) {
            return PANEL;
        }
        MediumReadingDraft draft = new MediumReadingDraft(code, position, answersFrom(params));
        session.setAttribute(DRAFT, draft);
        try {
            readings.check(assignment.get().questionnaireVersion(), draft.answers());
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return formPage(model, assignment.get(), position, draft);
        }
        Questionnaire questionnaire = questionnaireOf(assignment.get());
        model.addAttribute("pedido", assignment.get());
        model.addAttribute("posicao", position);
        model.addAttribute("nome", assignment.get().slot(position).orElseThrow().firstName());
        model.addAttribute("respostas", labelled(questionnaire, draft.answers()));
        return "leitura-revisar";
    }

    @PostMapping("/painel/medium/pedido/{code}/leitura/{position}/travar")
    String lock(Principal principal, @PathVariable String code, @PathVariable int position, HttpSession session) {
        MediumReadingDraft draft = (MediumReadingDraft) session.getAttribute(DRAFT);
        if (writable(principal, code, position).isEmpty()) {
            return PANEL;
        }
        if (draft == null || !draft.assignmentCode().equals(code) || draft.position() != position) {
            return "redirect:/painel/medium/pedido/" + code + "/leitura/" + position;
        }
        try {
            readings.lock(principal.getName(), code, position, draft.answers());
        } catch (IllegalArgumentException e) {
            // The draft in the session was never valid: back to the form, which shows why.
            return "redirect:/painel/medium/pedido/" + code + "/leitura/" + position;
        } catch (IllegalStateException e) {
            // Locked meanwhile, or past the deadline: the panel says where things stand.
            session.removeAttribute(DRAFT);
            return PANEL;
        }
        session.removeAttribute(DRAFT);
        return PANEL + "?travada";
    }

    /** The medium's own open request, with that position still unlocked. */
    private Optional<MediumAssignment> writable(Principal principal, String code, int position) {
        return readings.assignment(principal.getName(), code)
                .filter(MediumAssignment::open)
                .filter(assignment -> assignment.slot(position).filter(slot -> !slot.locked()).isPresent());
    }

    private String formPage(Model model, MediumAssignment assignment, int position, MediumReadingDraft draft) {
        model.addAttribute("pedido", assignment);
        model.addAttribute("posicao", position);
        model.addAttribute("nome", assignment.slot(position).orElseThrow().firstName());
        model.addAttribute("campos", questionnaireOf(assignment).scoredFields());
        model.addAttribute("versao", assignment.questionnaireVersion());
        model.addAttribute("rascunho", draft);
        model.addAttribute("naoRecebi", Questionnaire.NOT_RECEIVED);
        return "leitura";
    }

    private static Questionnaire questionnaireOf(MediumAssignment assignment) {
        return Questionnaires.byVersion(assignment.questionnaireVersion()).orElseThrow();
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

    /** Question label to answer label, in questionnaire order. */
    private static Map<String, String> labelled(Questionnaire questionnaire, Map<String, String> answers) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Questionnaire.Field field : questionnaire.scoredFields()) {
            String answer = answers.get(field.key());
            if (answer != null) {
                out.put(field.label(), questionnaire.labelOf(field.key(), answer));
            }
        }
        return out;
    }
}
