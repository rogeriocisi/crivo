package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.study.StudyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public pages. They say what the platform is and show the state of the audit trail, which
 * is public by design (P7), and nothing about participants, readings or allocations.
 */
@Controller
class HomeController {

    private final AuditTrail auditTrail;
    private final StudyService studies;

    HomeController(AuditTrail auditTrail, StudyService studies) {
        this.auditTrail = auditTrail;
        this.studies = studies;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("auditEvents", auditTrail.size());
        model.addAttribute("auditHead", auditTrail.headHash());
        model.addAttribute("studyCount", studies.all().size());
        return "index";
    }

    @GetMapping("/sobre")
    String about() {
        return "about";
    }
}
