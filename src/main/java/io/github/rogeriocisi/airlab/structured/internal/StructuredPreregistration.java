package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.analysis.AnalysisUnit;
import io.github.rogeriocisi.airlab.analysis.PrimaryMeasure;
import io.github.rogeriocisi.airlab.analysis.StoppingRule;
import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.structured.StructuredStudyTerms;
import io.github.rogeriocisi.airlab.study.PreregistrationPlan;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the P4 freezes besides the administrator's terms (SPEC E13, phase C): the protocol's fixed
 * text for the plan, and the annex with the whole questionnaire, the pairing rule, the draw rule,
 * the deadline and N. Everything here enters the hashed document.
 */
final class StructuredPreregistration {

    static final String PAIRING_CRITERIA = "Mesmo genero, mesma faixa etaria no obito e mesmo periodo do "
            + "falecimento; pedidos de consulentes diferentes; dentro do estrato, por ordem de travamento "
            + "(e pelo codigo, no empate), dois a dois, ate N pares ativos.";

    static final List<String> SCORING_SCALE = List.of(
            "Acerto: a resposta da leitura e igual a do consulente no campo.",
            "Erro: a resposta e diferente, ou o medium marcou \"nao recebi\".",
            "Campo fora da comparacao do par: algum dos dois consulentes marcou \"nao sei\"; vale igualmente "
                    + "para o alvo e para o chamariz.");

    static final String PRIMARY_ANALYSIS = "Por par (A, B), com as leituras R_A (feita com o nome de A) e R_B: "
            + "d = [s(R_A, A) - s(R_A, B)] + [s(R_B, B) - s(R_B, A)], onde s conta os campos comparaveis do par "
            + "em que a leitura coincide com as respostas do consulente. Teste de Wilcoxon exato unilateral "
            + "(d > 0) sobre os d dos pares concluidos.";

    static final List<String> SECONDARY_ANALYSES = List.of(
            "Binomial exato do sinal de d contra 0,5, entre os pares com d diferente de zero.",
            "Permutacao das leituras entre todos os alvos do estudo, dentro dos estratos do pareamento.");

    static final List<String> EXCLUSION_RULES = List.of(
            "Par desfeito pela retirada de um consulente: fora da analise.",
            "Pedido ao medium vencido ou cancelado: as leituras ja travadas dele ficam registradas e fora "
                    + "da analise; o par vai a outro medium sorteado.",
            "Pedido travado em versao do questionario diferente da congelada: nao entra no estudo.");

    private StructuredPreregistration() {
    }

    static PreregistrationPlan plan(StructuredStudyTerms terms) {
        return new PreregistrationPlan(
                List.of(terms.hypothesis().strip()),
                terms.mediumInclusionCriteria().strip(),
                PAIRING_CRITERIA,
                SCORING_SCALE,
                PRIMARY_ANALYSIS,
                PrimaryMeasure.PAIRED_FIELD_MATCHES,
                terms.expectedEffect(),
                AnalysisUnit.PAIR,
                StoppingRule.FIXED_SAMPLE_SIZE,
                terms.targetPairs(),
                // Both readings answer the same fields, so there is no length to balance.
                0,
                SECONDARY_ANALYSES,
                EXCLUSION_RULES);
    }

    static String annex(Questionnaire questionnaire, StructuredStudyTerms terms) {
        List<Object> fields = new ArrayList<>();
        for (Questionnaire.Field field : questionnaire.fields()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", field.key());
            row.put("label", field.label());
            row.put("use", field.use().name());
            row.put("outOfScoreReason", field.outOfScoreReason());
            List<Object> options = new ArrayList<>();
            for (Questionnaire.Option option : field.options()) {
                options.add(Map.of("key", option.key(), "label", option.label()));
            }
            row.put("options", options);
            fields.add(row);
        }
        Map<String, Object> questionnaireForm = new LinkedHashMap<>();
        questionnaireForm.put("version", questionnaire.version());
        questionnaireForm.put("fields", fields);
        questionnaireForm.put("sitterUnknown", Questionnaire.UNKNOWN);
        questionnaireForm.put("mediumNotReceived", Questionnaire.NOT_RECEIVED);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("protocol", "P4");
        root.put("questionnaire", questionnaireForm);
        root.put("pairingFields", List.of(Questionnaires.GENDER, Questionnaires.AGE_BAND, Questionnaires.DEATH_PERIOD));
        root.put("targetPairs", terms.targetPairs());
        root.put("mediumDeadlineDays", terms.deadlineDays());
        root.put("mediumAnswers", "scored fields only; each one an option or \"" + Questionnaire.NOT_RECEIVED + "\"");
        root.put("mediumDraw", "SeedStream(seed, \"p4-medium:\" + pair + \":\" + attempt).nextInt over the eligible "
                + "mediums in canonical order; eligible = confirmed, active medium accounts at the draw, minus "
                + "the mediums of earlier attempts of the pair");
        root.put("nameOrder", "SeedStream(seed, \"p4-name-order:\" + pair + \":\" + attempt).shuffle over the "
                + "two request codes in canonical order");
        root.put("readingCommitment", "SHA-256(CanonicalJson{answers, protocol, questionnaire, reading, request} "
                + "|| \"|\" || saltHex)");
        return CanonicalJson.write(root);
    }
}
