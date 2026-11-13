package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.analysis.PermutationResult;
import io.github.rogeriocisi.airlab.analysis.WilcoxonResult;
import io.github.rogeriocisi.airlab.catalog.ProtocolCatalog;
import io.github.rogeriocisi.airlab.structured.StructuredReport;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.study.StudySummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The canonical P4 report (SPEC E13, phase D): the exact text whose SHA-256 goes into the trail.
 *
 * <p>Like the preregistration document: no timestamp, no locale-dependent formatting, always LF,
 * so the same data gives the same bytes on any machine. Neutral wording (P3): the primary result
 * is said to be, or not to be, below 0.05 — a statement about the test, never about the origin of
 * the information.
 */
final class StructuredReportDocument {

    record Content(StudySummary study,
                   String questionnaireVersion,
                   String preregistrationSha256,
                   String seedCommitment,
                   String seedHex,
                   List<StructuredReport.PairResult> pairs,
                   WilcoxonResult primary,
                   int signPositive,
                   int signTrials,
                   BigDecimal signP,
                   PermutationResult permutation,
                   int sitterVerified,
                   int sitterTotal,
                   int readingVerified,
                   int readingTotal,
                   long dissolvedPairs,
                   long expiredAssignments,
                   long cancelledAssignments) {
    }

    private StructuredReportDocument() {
    }

    static String render(Content content) {
        StringBuilder out = new StringBuilder();
        line(out, "# Relatorio — " + content.study().title());
        line(out, "");
        line(out, "- Estudo: " + content.study().code());
        line(out, "- Protocolo: " + StructuredStudyService.PROTOCOL);
        line(out, "- Questionario: " + content.questionnaireVersion());
        line(out, "- Pre-registro congelado (SHA-256): " + content.preregistrationSha256());
        line(out, "- Compromisso da semente: " + content.seedCommitment());
        line(out, "- Semente revelada: " + content.seedHex());
        line(out, "- Pares analisados: " + content.pairs().size());
        line(out, "");

        WilcoxonResult primary = content.primary();
        boolean below = primary.pValue().compareTo(StructuredReport.ALPHA) <= 0;
        line(out, "## Analise primaria (pre-registrada)");
        line(out, "");
        line(out, "Wilcoxon unilateral sobre os d dos pares, hipotese d > 0.");
        line(out, "");
        line(out, "- Pares com d diferente de zero: " + primary.pairs());
        line(out, "- Pares com d = 0, fora do teste: " + primary.droppedZeros());
        line(out, "- W+: " + primary.positiveRankSum());
        line(out, "- p: " + StructuredUnblinding.plain(primary.pValue()));
        line(out, "- Metodo: " + primary.method());
        line(out, "- Leitura: o p " + (below ? "esta" : "nao esta") + " abaixo de 0,05. E uma afirmacao sobre o "
                + "teste, nao sobre a origem da informacao.");
        line(out, "");

        line(out, "## Analises secundarias (pre-registradas)");
        line(out, "");
        line(out, "- Binomial exato do sinal: d > 0 em " + content.signPositive() + " de " + content.signTrials()
                + " par(es) com d diferente de zero; p = " + StructuredUnblinding.plain(content.signP()));
        PermutationResult permutation = content.permutation();
        line(out, "- Permutacao das leituras dentro dos estratos: soma observada " + permutation.observedDiagonalSum()
                + ", " + permutation.permutations() + " arranjo(s), p = "
                + StructuredUnblinding.plain(permutation.pValue()) + ". " + permutation.method());
        line(out, "");

        line(out, "## Integridade");
        line(out, "");
        line(out, "- Compromissos dos pedidos conferidos: " + content.sitterVerified() + " de " + content.sitterTotal());
        line(out, "- Compromissos das leituras conferidos: " + content.readingVerified() + " de " + content.readingTotal());
        line(out, "");

        line(out, "## Exclusoes (regras do pre-registro)");
        line(out, "");
        line(out, "- Pares desfeitos por retirada: " + content.dissolvedPairs());
        line(out, "- Pedidos ao medium vencidos: " + content.expiredAssignments());
        line(out, "- Pedidos ao medium cancelados: " + content.cancelledAssignments());
        line(out, "");

        line(out, "## Por par");
        line(out, "");
        line(out, "| Par | Medium | Campos comparaveis | s(RA,A) | s(RA,B) | s(RB,B) | s(RB,A) | d |");
        line(out, "|---|---|---|---|---|---|---|---|");
        for (StructuredReport.PairResult pair : content.pairs()) {
            line(out, "| " + pair.pairCode() + " | " + pair.mediumCode() + " | " + pair.comparableFields() + " | "
                    + pair.targetMatchesA() + " | " + pair.decoyMatchesA() + " | " + pair.targetMatchesB() + " | "
                    + pair.decoyMatchesB() + " | " + pair.d() + " |");
        }
        line(out, "");

        line(out, "## Por medium");
        line(out, "");
        line(out, "| Medium | Pares | Soma de d |");
        line(out, "|---|---|---|");
        Map<String, int[]> byMedium = new TreeMap<>();
        for (StructuredReport.PairResult pair : content.pairs()) {
            int[] totals = byMedium.computeIfAbsent(pair.mediumCode(), key -> new int[2]);
            totals[0]++;
            totals[1] += pair.d();
        }
        byMedium.forEach((medium, totals) -> line(out, "| " + medium + " | " + totals[0] + " | " + totals[1] + " |"));
        line(out, "");

        line(out, "## O que este resultado nao sustenta");
        line(out, "");
        List<String> limits = ProtocolCatalog.structuredReading().limits();
        for (int i = 0; i < limits.size(); i++) {
            line(out, (i + 1) + ". " + limits.get(i));
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        // Always LF: the hash must not depend on the operating system.
        out.append(text).append('\n');
    }
}
