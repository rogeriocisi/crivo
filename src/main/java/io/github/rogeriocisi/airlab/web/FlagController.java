package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.FlagDecision;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The administrator's review of linked-accounts flags (SPEC E13, phase E): codes, profiles and the
 * kind of signal — no name, no address. A flag never blocks anyone; the decision only says whether
 * the draw keeps the two accounts apart.
 */
@Controller
class FlagController {

    private static final String PAGE = "/painel/adm/sinalizacoes";

    private final AccountService accounts;

    FlagController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping(PAGE)
    String flags(Model model) {
        model.addAttribute("sinalizacoes", accounts.flags());
        return "sinalizacoes";
    }

    @PostMapping(PAGE + "/{code}")
    String review(@PathVariable String code, @RequestParam("decisao") FlagDecision decision, Principal principal) {
        try {
            accounts.reviewFlag(code, decision, principal.getName());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Unknown, or decided meanwhile in another tab: the list shows where it stands.
            return "redirect:" + PAGE;
        }
        return "redirect:" + PAGE + "?revisada";
    }
}
