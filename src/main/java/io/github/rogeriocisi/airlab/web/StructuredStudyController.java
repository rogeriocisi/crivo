package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.analysis.PowerCheck;
import io.github.rogeriocisi.airlab.analysis.PrimaryMeasure;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyTerms;
import io.github.rogeriocisi.airlab.structured.StructuredStudyView;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The administrator's P4 study screen (SPEC E13, phase C): fill in the preregistration, review it
 * with the declared power, and freeze it — one step, with no unfrozen version stored. Afterwards
 * the same screen shows the frozen document, its hash, the seed commitment and the counts.
 */
@Controller
class StructuredStudyController {

    static final String DRAFT = "p4.estudo";
    private static final String PAGE = "/painel/adm/estudo-p4";

    private final StructuredStudyService studies;

    StructuredStudyController(StructuredStudyService studies) {
        this.studies = studies;
    }

    @GetMapping(PAGE)
    String page(HttpSession session, Model model) {
        Optional<StructuredStudyView> current = studies.current();
        if (current.isPresent()) {
            model.addAttribute("estudo", current.get());
            model.addAttribute("progresso", studies.progress());
            return "estudo-p4";
        }
        StructuredStudyTerms draft = (StructuredStudyTerms) session.getAttribute(DRAFT);
        return formPage(model, draft == null ? StructuredStudyTerms.defaults() : draft);
    }

    /** A P4 study by its code: the open one, or a concluded one with its report (phase D). */
    @GetMapping(PAGE + "/{code}")
    String study(@PathVariable String code, Model model) {
        Optional<StructuredStudyView> study = studies.byCode(code);
        if (study.isEmpty()) {
            return "redirect:" + PAGE;
        }
        model.addAttribute("estudo", study.get());
        model.addAttribute("progresso", studies.progress());
        model.addAttribute("relatorio", studies.report(code).orElse(null));
        return "estudo-p4";
    }

    @PostMapping(PAGE + "/revisar")
    String review(@RequestParam(name = "titulo", defaultValue = "") String title,
                  @RequestParam(name = "pares", defaultValue = "0") int targetPairs,
                  @RequestParam(name = "prazo", defaultValue = "0") int deadlineDays,
                  @RequestParam(name = "efeito", defaultValue = "0") double expectedEffect,
                  @RequestParam(name = "hipotese", defaultValue = "") String hypothesis,
                  @RequestParam(name = "criterios", defaultValue = "") String criteria,
                  @RequestParam(name = "teto", defaultValue = "0") int maxPairsPerMedium,
                  HttpSession session, Model model) {
        if (studies.current().isPresent()) {
            return "redirect:" + PAGE;
        }
        StructuredStudyTerms terms = new StructuredStudyTerms(title, targetPairs, deadlineDays, expectedEffect,
                hypothesis, criteria, maxPairsPerMedium);
        session.setAttribute(DRAFT, terms);
        try {
            studies.check(terms);
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return formPage(model, terms);
        }
        model.addAttribute("termos", terms);
        model.addAttribute("poder", PowerCheck.assess(PrimaryMeasure.PAIRED_FIELD_MATCHES,
                terms.expectedEffect(), terms.targetPairs()));
        addQuestionnaire(model);
        return "estudo-p4-revisar";
    }

    @PostMapping(PAGE + "/congelar")
    String freeze(Principal principal, HttpSession session, RedirectAttributes redirect) {
        StructuredStudyTerms terms = (StructuredStudyTerms) session.getAttribute(DRAFT);
        if (terms == null || studies.current().isPresent()) {
            return "redirect:" + PAGE;
        }
        try {
            studies.open(terms, principal.getName());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + PAGE;
        }
        session.removeAttribute(DRAFT);
        return "redirect:" + PAGE + "?congelado";
    }

    private String formPage(Model model, StructuredStudyTerms terms) {
        model.addAttribute("termos", terms);
        model.addAttribute("prazoMaximo", StructuredStudyTerms.MAXIMUM_DEADLINE_DAYS);
        model.addAttribute("paresMaximo", StructuredStudyTerms.MAXIMUM_TARGET_PAIRS);
        model.addAttribute("concluidos", studies.concluded());
        addQuestionnaire(model);
        return "estudo-p4-novo";
    }

    private void addQuestionnaire(Model model) {
        String version = Questionnaires.current().version();
        model.addAttribute("questionario", Questionnaires.current());
        model.addAttribute("rascunhoBloqueado", version.endsWith("-rascunho") && !studies.draftQuestionnaireAllowed());
    }
}
