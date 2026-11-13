package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyView;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The public page of the P4 studies (SPEC E13, phase E): what was frozen, with its hash; counts
 * while a study collects; the whole report once it is unblinded. Only what is public by
 * construction (P2, P7) — the report does not exist before the unblinding, so there is nothing to
 * hide early.
 */
@Controller
class PublicStudiesController {

    private final StructuredStudyService studies;

    PublicStudiesController(StructuredStudyService studies) {
        this.studies = studies;
    }

    @GetMapping("/estudos")
    String list(Model model) {
        model.addAttribute("aberto", studies.current().orElse(null));
        model.addAttribute("concluidos", studies.concluded());
        return "estudos";
    }

    @GetMapping("/estudos/{code}")
    String study(@PathVariable String code, Model model) {
        Optional<StructuredStudyView> study = studies.byCode(code);
        if (study.isEmpty()) {
            return "redirect:/estudos";
        }
        boolean open = studies.current().map(current -> current.studyCode().equals(code)).orElse(false);
        model.addAttribute("estudo", study.get());
        model.addAttribute("progresso", open ? studies.progress() : null);
        model.addAttribute("relatorio", studies.report(code).orElse(null));
        return "estudo-publico";
    }
}
