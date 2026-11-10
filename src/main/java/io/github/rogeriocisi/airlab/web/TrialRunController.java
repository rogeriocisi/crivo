package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.trials.SessionOutcome;
import io.github.rogeriocisi.airlab.trials.SessionProgress;
import io.github.rogeriocisi.airlab.trials.TrialProtocol;
import io.github.rogeriocisi.airlab.trials.TrialSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.rogeriocisi.airlab.trials.TrialOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Runs a self-service test in the browser (E11, E12).
 *
 * <p>Anonymous on purpose: no account, no e-mail, no personal data, because the public pool is
 * exploratory data and there is nothing here worth asking a person to hand over. The session
 * code is kept in the HTTP session, so a reload resumes the test rather than starting a new
 * one and quietly inflating the number of abandoned sessions.
 *
 * <p>While a session runs, the only thing these pages can show is how many trials are left.
 * The result page exists for a closed session and nothing else.
 */
@Controller
class TrialRunController {

    private static final String SESSION_KEY_PREFIX = "airlab.trial.";

    /** Version of the downloadable session receipt; bump it when a field changes meaning. */
    static final String RECEIPT_FORMAT = "crivo.sessao/1";

    private static final ObjectMapper RECEIPT_JSON =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final TrialSessionService trialSessions;

    TrialRunController(TrialSessionService trialSessions) {
        this.trialSessions = trialSessions;
    }

    @GetMapping("/testes/{slug}/rodar")
    String run(@PathVariable String slug, HttpSession http, Model model) {
        TrialProtocol protocol = protocolOf(slug);
        String code = (String) http.getAttribute(key(protocol));
        if (code == null) {
            model.addAttribute("protocolo", protocol);
            model.addAttribute("tentativasPadrao", protocol.defaultTrials());
            return view(protocol, "convite");
        }

        SessionProgress progress = trialSessions.progress(code);
        if (progress.finished() || progress.state().name().equals("ABANDONED")) {
            return "redirect:/testes/" + slug + "/resultado";
        }
        return trialView(protocol, progress, model);
    }

    @PostMapping("/testes/{slug}/comecar")
    String start(@PathVariable String slug,
                 @RequestParam(defaultValue = "0") int tentativas,
                 HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        int declared = tentativas > 0 ? tentativas : protocol.defaultTrials();
        SessionProgress progress = trialSessions.start(protocol, declared);
        http.setAttribute(key(protocol), progress.sessionCode());
        return "redirect:/testes/" + slug + "/rodar";
    }

    @PostMapping("/testes/{slug}/entropia")
    String entropy(@PathVariable String slug,
                   @RequestParam String nonce,
                   HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        trialSessions.contributeEntropy(requireCode(protocol, http), nonce);
        return "redirect:/testes/" + slug + "/rodar";
    }

    @PostMapping("/testes/{slug}/impressao")
    String impression(@PathVariable String slug,
                      @RequestParam String impressao,
                      HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        trialSessions.lockImpression(requireCode(protocol, http), impressao);
        return "redirect:/testes/" + slug + "/rodar";
    }

    @PostMapping("/testes/{slug}/escolha")
    String choice(@PathVariable String slug,
                  @RequestParam int posicao,
                  HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        String code = requireCode(protocol, http);
        SessionProgress progress = trialSessions.submitChoice(code, posicao);
        return afterTrial(slug, code, progress);
    }

    @PostMapping("/testes/{slug}/ordenacao")
    String ranking(@PathVariable String slug,
                   @RequestParam List<Integer> ordem,
                   HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        String code = requireCode(protocol, http);
        SessionProgress progress = trialSessions.submitRanking(code, new ArrayList<>(ordem));
        return afterTrial(slug, code, progress);
    }

    @PostMapping("/testes/{slug}/desistir")
    String abandon(@PathVariable String slug, HttpSession http) {
        TrialProtocol protocol = protocolOf(slug);
        trialSessions.abandon(requireCode(protocol, http));
        return "redirect:/testes/" + slug + "/resultado";
    }

    @GetMapping("/testes/{slug}/resultado")
    String result(@PathVariable String slug, HttpSession http, Model model) {
        TrialProtocol protocol = protocolOf(slug);
        String code = (String) http.getAttribute(key(protocol));
        if (code == null) {
            return "redirect:/testes/" + slug;
        }
        SessionOutcome outcome = trialSessions.outcome(code)
                .orElseThrow(() -> new IllegalStateException(
                        "a sessao ainda esta aberta; o desfecho aparece de uma vez, no fim"));
        // Clearing it here means the next visit starts a fresh session instead of reopening a
        // closed one, and the person keeps the session code to verify it later.
        http.removeAttribute(key(protocol));
        model.addAttribute("protocolo", protocol);
        model.addAttribute("desfecho", outcome);
        model.addAttribute("comprovante", receipt(outcome));
        return "teste-resultado";
    }

    /**
     * A repeated or stale submission resumes the test instead of becoming an error page.
     *
     * <p>Reloading, double clicking or going back are ordinary things people do, and in this
     * flow they all mean the same: the browser is asking for a step the server has already
     * passed. The server state is the authoritative one, so the answer is to render the step
     * the session is actually on. Nothing is accepted twice — the refusal happened in the
     * domain, which is where it belongs — and the person is not sent to a login screen for it.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    String resumeAfterConflict(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (TrialProtocol protocol : TrialProtocol.values()) {
            if (path.contains("/testes/" + protocol.slug() + "/")) {
                return "redirect:/testes/" + protocol.slug() + "/rodar";
            }
        }
        return "redirect:/testes";
    }

    /**
     * The closed session as a downloadable receipt (P7): the same values the result page shows,
     * in a stable shape a third party can feed to their own verifier. Built only from a
     * {@link SessionOutcome}, which exists only once the session is over.
     *
     * <p>The JSON goes inside a {@code <script type="application/json">} element, so every
     * {@code <} is escaped: no value can close that element early.
     */
    static String receipt(SessionOutcome outcome) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("formato", RECEIPT_FORMAT);
        json.put("sessao", outcome.sessionCode());
        json.put("protocolo", outcome.protocol().name());
        json.put("situacao", outcome.state().name());
        json.put("tentativasDeclaradas", outcome.declaredTrials());
        json.put("tentativasTravadas", outcome.lockedTrials());
        json.put("alternativas", outcome.chanceDenominator());
        json.put("acaso", outcome.chanceNumerator() + "/" + outcome.chanceDenominator());
        json.put("acertos", outcome.hits());
        json.put("valorP", outcome.pValue());
        json.put("compromissoDaSemente", outcome.seedCommitment());
        json.put("metadeDoServidor", outcome.serverSeedHex());
        json.put("metadeDoParticipante", outcome.participantNonce());
        json.put("sementeEfetiva", outcome.seedHex());
        json.put("bancoDeFiguras", outcome.targetPoolVersion());
        json.put("inicio", outcome.startedAt() == null ? null : outcome.startedAt().toString());
        json.put("fim", outcome.closedAt() == null ? null : outcome.closedAt().toString());
        json.put("regra", List.of(
                "semente = SHA-256(metade do servidor || metade do participante)",
                "alvo(n) = SHA-256(semente || \"trial:\" || n || contador) mod alternativas"));
        List<Map<String, Object>> trials = new ArrayList<>();
        for (TrialOutcome trial : outcome.trials()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("n", trial.index());
            t.put("resposta", trial.chosenIndex());
            t.put("alvo", trial.targetIndex());
            t.put("acerto", trial.hit());
            if (!trial.candidateSlugs().isEmpty()) {
                t.put("figuras", trial.candidateSlugs());
            }
            trial.impressionSha256().ifPresent(hash -> t.put("hashDaImpressao", hash));
            trials.add(t);
        }
        json.put("tentativas", trials);
        json.put("natureza", outcome.exploratoryReason());
        try {
            return RECEIPT_JSON.writeValueAsString(json).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("comprovante da sessao nao serializou", e);
        }
    }

    private String afterTrial(String slug, String code, SessionProgress progress) {
        if (progress.remainingTrials() == 0) {
            trialSessions.finish(code);
            return "redirect:/testes/" + slug + "/resultado";
        }
        return "redirect:/testes/" + slug + "/rodar";
    }

    private String trialView(TrialProtocol protocol, SessionProgress progress, Model model) {
        model.addAttribute("protocolo", protocol);
        model.addAttribute("progresso", progress);
        if (progress.awaitingEntropy()) {
            // Nothing exists yet: no seed, no targets, no trial. The session is waiting for the
            // participant's half, drawn in their browser now that the commitment is on screen.
            return "teste-entropia";
        }
        if (protocol.impressionRequired()) {
            if (progress.awaitingImpression()) {
                return view(protocol, "impressao");
            }
            model.addAttribute("candidatas", trialSessions.currentCandidates(progress.sessionCode()));
            return view(protocol, "ordenacao");
        }
        model.addAttribute("alternativas", protocol.alternatives());
        return view(protocol, "escolha");
    }

    private static String view(TrialProtocol protocol, String step) {
        return "teste-" + protocol.slug() + "-" + step;
    }

    private String requireCode(TrialProtocol protocol, HttpSession http) {
        String code = (String) http.getAttribute(key(protocol));
        if (code == null) {
            throw new IllegalStateException("nenhuma sessao aberta neste navegador");
        }
        return code;
    }

    private static String key(TrialProtocol protocol) {
        return SESSION_KEY_PREFIX + protocol.name();
    }

    private static TrialProtocol protocolOf(String slug) {
        for (TrialProtocol protocol : TrialProtocol.values()) {
            if (protocol.slug().equals(slug)) {
                return protocol;
            }
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
