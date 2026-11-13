package io.github.rogeriocisi.airlab.study.internal;

import io.github.rogeriocisi.airlab.analysis.PowerAssessment;
import io.github.rogeriocisi.airlab.analysis.PowerCheck;
import io.github.rogeriocisi.airlab.study.PreregistrationPlan;
import java.util.List;

/**
 * Builds the canonical preregistration document: the exact text whose SHA-256 is published
 * before the first session (Constitution P2, Epic E1).
 *
 * <p>Deliberately free of timestamps, locale dependent formatting and anything else that
 * would make the same plan hash differently on another machine. It is Markdown so that a
 * human auditor can read it, and it ends with the canonical JSON of the plan so that a
 * machine can compare it field by field.
 */
final class PreregistrationDocument {

    private PreregistrationDocument() {
    }

    /**
     * @param protocolAnnex canonical JSON that the protocol adds to the plan, or null. The P1
     *                      has none, so its documents are byte for byte what they were before
     *                      the annex existed.
     */
    static String render(String studyCode, String title, String protocol, int version,
                         PreregistrationPlan plan, String allocationCommitment,
                         String amendmentReason, String protocolAnnex) {
        StringBuilder out = new StringBuilder();
        line(out, "# Pre-registro — " + title);
        line(out, "");
        line(out, "- Estudo: " + studyCode);
        line(out, "- Protocolo: " + protocol);
        line(out, "- Versao do pre-registro: " + version);
        line(out, "- Compromisso do sorteio (SHA-256): "
                + (allocationCommitment == null ? "(nao congelado)" : allocationCommitment));
        if (amendmentReason != null) {
            line(out, "- Justificativa da emenda: " + amendmentReason);
        }
        line(out, "");
        section(out, "Hipoteses", plan.hypotheses());
        line(out, "## Criterios de inclusao e triagem de mediuns");
        line(out, "");
        line(out, plan.mediumInclusionCriteria());
        line(out, "");
        line(out, "## Criterio de pareamento");
        line(out, "");
        line(out, plan.pairingCriteria());
        line(out, "");
        section(out, "Escala de pontuacao dos itens", plan.scoringScale());
        line(out, "## Analise primaria (confirmatoria)");
        line(out, "");
        line(out, plan.primaryAnalysis());
        line(out, "");
        line(out, "- Medida primaria: " + plan.primaryMeasure().name());
        line(out, "- Efeito esperado (base do calculo de amostra): " + plan.expectedEffect());
        line(out, "");
        section(out, "Analises secundarias", plan.secondaryAnalyses());
        section(out, "Regras de exclusao", plan.exclusionRules());
        line(out, "## Unidade de analise e regra de parada");
        line(out, "");
        line(out, "- Unidade de analise: " + plan.analysisUnit().name());
        line(out, "- Regra de parada: " + plan.stoppingRule().name());
        line(out, "- Tamanho-alvo da amostra: " + plan.targetSampleSize() + " unidade(s) independente(s)");
        line(out, "- Diferenca maxima de itens entre as duas leituras de um par: "
                + plan.maxItemCountDifference());
        line(out, "");
        // Poder: declarado no proprio documento congelado, para que um resultado nulo nao possa
        // depois ser apresentado como ausencia de efeito se o desenho nunca pudesse detecta-lo.
        PowerAssessment power = PowerCheck.assess(
                plan.primaryMeasure(), plan.expectedEffect(), plan.targetSampleSize());
        line(out, "## Poder estatistico declarado");
        line(out, "");
        line(out, "- Minimo para o efeito declarado (alfa 0,05 unilateral, poder 0,80): "
                + power.requiredPairs() + " unidade(s)");
        line(out, "- Suficiente: " + (power.adequate() ? "sim" : "NAO"));
        line(out, "- " + power.detail());
        line(out, "");
        if (protocolAnnex != null) {
            line(out, "## Anexo do protocolo (forma canonica)");
            line(out, "");
            line(out, protocolAnnex);
            line(out, "");
        }
        line(out, "## Forma canonica do plano");
        line(out, "");
        line(out, plan.canonicalForm());
        return out.toString();
    }

    private static void section(StringBuilder out, String heading, List<String> items) {
        line(out, "## " + heading);
        line(out, "");
        if (items.isEmpty()) {
            line(out, "(nenhuma)");
        } else {
            for (int i = 0; i < items.size(); i++) {
                line(out, (i + 1) + ". " + items.get(i));
            }
        }
        line(out, "");
    }

    private static void line(StringBuilder out, String text) {
        // Always a single LF, never the platform separator: the hash must not depend on
        // whether the document was written on Windows or Linux.
        out.append(text).append('\n');
    }
}
