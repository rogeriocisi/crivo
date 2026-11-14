package io.github.rogeriocisi.airlab.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rogeriocisi.airlab.allocation.AllocationEngine;
import io.github.rogeriocisi.airlab.anchor.AnchorCheck;
import io.github.rogeriocisi.airlab.anchor.AnchorService;
import io.github.rogeriocisi.airlab.allocation.AllocationPlan;
import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.allocation.AllocationSnapshot;
import io.github.rogeriocisi.airlab.allocation.DrawInput;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.audit.ChainVerification;
import io.github.rogeriocisi.airlab.study.PreregistrationSnapshot;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudySummary;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@code airlab verify}: the command line verification required by Constitution P4 and P5
 * and by Epic E8.
 *
 * <p>It checks three independent things and prints each one, so a sceptical auditor can see
 * which guarantee holds and which does not:
 *
 * <ol>
 *   <li>the audit chain recomputes to the stored hashes;</li>
 *   <li>each preregistration document still hashes to its published SHA-256;</li>
 *   <li>for every study whose seed is revealed, rerunning the draw from the seed reproduces
 *       the stored allocations and the published commitment;</li>
 *   <li>every external time stamp is signed by the pinned authority over exactly the hash of the
 *       event at its position in the trail (SPEC E8, v0.15).</li>
 * </ol>
 *
 * <p>Exit code 0 when everything checks out, 1 otherwise, so that a scheduled run on either
 * Windows or Linux can fail loudly.
 */
@Component
class VerifyRunner implements ApplicationRunner {

    private final AuditTrail auditTrail;
    private final StudyService studies;
    private final AllocationService allocations;
    private final ObjectMapper objectMapper;
    private final ApplicationContext context;
    private final AnchorService anchors;

    VerifyRunner(AuditTrail auditTrail,
                 StudyService studies,
                 AllocationService allocations,
                 ObjectMapper objectMapper,
                 ApplicationContext context,
                 AnchorService anchors) {
        this.auditTrail = auditTrail;
        this.studies = studies;
        this.allocations = allocations;
        this.objectMapper = objectMapper;
        this.context = context;
        this.anchors = anchors;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.getNonOptionArgs().contains("verify")) {
            return;
        }
        Optional<String> onlyStudy = args.getOptionValues("study") == null
                ? Optional.empty()
                : args.getOptionValues("study").stream().findFirst();

        List<String> failures = new ArrayList<>();
        println("Crivo (AIR Lab) - verificacao");
        println("");

        ChainVerification chain = auditTrail.verifyChain();
        println("1) Trilha de auditoria");
        println("   " + chain.detail());
        if (!chain.intact()) {
            failures.add("trilha de auditoria");
        }
        println("");

        println("2) Pre-registro e sorteio");
        List<StudySummary> list = studies.all().stream()
                .filter(s -> onlyStudy.isEmpty() || onlyStudy.get().equals(s.code()))
                .toList();
        if (list.isEmpty()) {
            println("   Nenhum estudo para verificar.");
        }
        for (StudySummary study : list) {
            println("   Estudo " + study.code() + " (" + study.status() + ")");
            for (PreregistrationSnapshot version : studies.preregistrationHistory(study.id())) {
                String recomputed = Hashes.sha256Hex(version.document());
                boolean ok = recomputed.equals(version.sha256());
                println("     - pre-registro v" + version.version()
                        + (version.frozen() ? " (congelado)" : " (rascunho)")
                        + ": " + (ok ? "hash confere" : "HASH NAO CONFERE"));
                if (!ok) {
                    failures.add("pre-registro v" + version.version() + " do estudo " + study.code());
                }
            }
            verifyDraw(study, failures);
        }
        println("");

        println("3) Carimbos de tempo externos");
        List<AnchorCheck> checks = anchors.verifyAll();
        if (checks.isEmpty()) {
            println("   Nenhum carimbo concedido ainda.");
        }
        for (AnchorCheck check : checks) {
            println("   " + check.code() + " (posicao " + check.anchoredPosition() + "): " + check.detail());
            if (!check.ok()) {
                failures.add("carimbo " + check.code());
            }
        }
        println("");

        boolean ok = failures.isEmpty();
        println(ok
                ? "Resultado: tudo confere."
                : "Resultado: falha em " + String.join(", ", failures) + ".");
        exit(ok ? 0 : 1);
    }

    private void verifyDraw(StudySummary study, List<String> failures) {
        Optional<AllocationSnapshot> maybe = allocations.snapshot(study.id());
        if (maybe.isEmpty()) {
            println("     - sorteio: ainda nao realizado");
            return;
        }
        AllocationSnapshot snapshot = maybe.get();
        if (snapshot.seedHex().isEmpty()) {
            println("     - sorteio: compromisso publicado, semente ainda sob sigilo "
                    + "(so e verificavel depois do desmascaramento)");
            return;
        }
        try {
            DrawInput input = objectMapper.readValue(snapshot.canonicalDrawInput(), DrawInput.class);
            AllocationPlan replayed = AllocationEngine.plan(snapshot.seedHex().get(), input);
            boolean sameAllocations = replayed.canonicalForm().equals(snapshot.canonicalAllocations());
            boolean sameCommitment = AllocationEngine
                    .commitment(snapshot.seedHex().get(), replayed)
                    .equals(snapshot.commitment());
            println("     - sorteio reproduzido da semente: "
                    + (sameAllocations ? "alocacoes conferem" : "ALOCACOES NAO CONFEREM"));
            println("     - compromisso publicado: "
                    + (sameCommitment ? "confere" : "NAO CONFERE"));
            if (!sameAllocations || !sameCommitment) {
                failures.add("sorteio do estudo " + study.code());
            }
        } catch (Exception e) {
            println("     - sorteio: nao foi possivel reproduzir (" + e.getClass().getSimpleName() + ")");
            failures.add("sorteio do estudo " + study.code());
        }
    }

    private void println(String text) {
        // The CLI talks to a person in a terminal, so it writes to stdout rather than to the log.
        System.out.println(text);
    }

    private void exit(int code) {
        org.springframework.boot.SpringApplication.exit(context, (ExitCodeGenerator) () -> code);
        if (code != 0) {
            System.exit(code);
        }
    }
}
