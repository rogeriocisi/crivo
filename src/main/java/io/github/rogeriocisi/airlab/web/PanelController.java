package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.Mailer;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.AccountView;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * One panel per profile (SPEC E13, "Telas por perfil"). Who reaches which panel is decided in
 * the security configuration; here each panel gets a model built for that profile alone.
 *
 * <p>The administrator panel shows counts and notices, never a score: in phase A there are no
 * scores at all, and when there are, they will not exist before unblinding (E10, lock 1).
 */
@Controller
class PanelController {

    private final AccountService accounts;
    private final ConsultationService consultations;
    private final AdminNotices notices;
    private final Mailer mailer;
    private final SignUpGate gate;
    private final Environment environment;

    PanelController(AccountService accounts, ConsultationService consultations, AdminNotices notices, Mailer mailer,
                    SignUpGate gate, Environment environment) {
        this.accounts = accounts;
        this.consultations = consultations;
        this.notices = notices;
        this.mailer = mailer;
        this.gate = gate;
        this.environment = environment;
    }

    /** Sends each person to the panel of their profile. */
    @GetMapping("/painel")
    String route(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN" -> {
                    return "redirect:/painel/adm";
                }
                case "ROLE_SITTER" -> {
                    return "redirect:/painel/consulente";
                }
                case "ROLE_MEDIUM" -> {
                    return "redirect:/painel/medium";
                }
                default -> {
                }
            }
        }
        return "redirect:/";
    }

    @GetMapping("/painel/consulente")
    String sitter(Principal principal, Model model, HttpServletRequest request) throws ServletException {
        model.addAttribute("situacao", consultations.status(principal.getName()).orElse(null));
        model.addAttribute("pedidosAbertos", gate.requestsOpen());
        return ownPanel(principal, model, request, "painel-consulente");
    }

    @GetMapping("/painel/medium")
    String medium(Principal principal, Model model, HttpServletRequest request) throws ServletException {
        return ownPanel(principal, model, request, "painel-medium");
    }

    private String ownPanel(Principal principal, Model model, HttpServletRequest request, String view)
            throws ServletException {
        Optional<AccountView> account = accounts.account(principal.getName());
        if (account.isEmpty()) {
            // Closed in another session: nothing left to show.
            request.logout();
            return "redirect:/login";
        }
        model.addAttribute("conta", account.get());
        return view;
    }

    @GetMapping("/painel/adm")
    String admin(Model model) {
        model.addAttribute("contas", accounts.counts());
        model.addAttribute("pedidos", consultations.counts());
        model.addAttribute("pedidosAbertos", gate.requestsOpen());
        model.addAttribute("avisos", notices.recent(50));
        model.addAttribute("caixaLocal", mailer.localOutbox());
        model.addAttribute("emailAtivo", mailer.enabled());
        model.addAttribute("inscricaoAberta", gate.open());
        model.addAttribute("avisoPorEmail",
                !environment.getProperty("airlab.admin-email", "").isBlank() && mailer.enabled());
        return "painel-adm";
    }

    /** The local outbox: in the local profile no e-mail leaves the machine; it waits here. */
    @GetMapping("/caixa-de-saida")
    String outbox(Model model) {
        model.addAttribute("caixaLocal", mailer.localOutbox());
        model.addAttribute("mensagens", mailer.outbox());
        return "caixa-de-saida";
    }
}
