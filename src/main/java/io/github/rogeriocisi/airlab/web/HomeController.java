package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.study.StudyService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public pages. They say what the platform is, how to audit it and where the code lives, and
 * show the state of the audit trail, which is public by design (P7). Nothing here says
 * anything about participants, readings or allocations.
 */
@Controller
class HomeController {

    private final AuditTrail auditTrail;
    private final StudyService studies;
    private final Environment environment;

    HomeController(AuditTrail auditTrail, StudyService studies, Environment environment) {
        this.auditTrail = auditTrail;
        this.studies = studies;
        this.environment = environment;
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

    /**
     * Page for the person who will rate the readings. Separate from the rest of the site on
     * purpose: they arrive by an invitation link, usually grieving, and need the procedure and
     * their rights in plain words, not the argument for the platform (P6).
     */
    @GetMapping("/consulente")
    String participant() {
        return "consulente";
    }

    @GetMapping("/historia")
    String history() {
        return "historia";
    }

    @GetMapping("/referencias")
    String references() {
        return "references";
    }

    /**
     * Sign-in page for whoever runs this installation.
     *
     * <p>It tells the visitor what the login is actually for, because today there is no
     * researcher screen behind it: the account exists to protect what comes next, and the
     * public pages are the whole surface. Saying so is better than a bare form that looks
     * like a locked door with a room behind it.
     */
    @GetMapping("/login")
    String login(Model model) {
        model.addAttribute("localProfile", environment.matchesProfiles("local", "test"));
        return "login";
    }
}
