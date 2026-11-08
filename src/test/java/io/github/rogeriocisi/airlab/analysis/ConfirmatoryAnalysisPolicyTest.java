package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rogeriocisi.airlab.study.AnalysisUnit;
import io.github.rogeriocisi.airlab.study.StoppingRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Constitution P2: an analysis that is not exactly the preregistered one is exploratory, and
 * says so. These tests pin the answer to the "optional stopping" criticism.
 */
class ConfirmatoryAnalysisPolicyTest {

    @Test
    @DisplayName("no alvo exato, com pre-registro congelado e unidade por par, e confirmatoria")
    void exactly_the_declared_sample_is_confirmatory() {
        AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                true, AnalysisUnit.PAIR, StoppingRule.FIXED_SAMPLE_SIZE, 70, 70);

        assertThat(eligibility.confirmatory()).isTrue();
        assertThat(eligibility.justification()).contains("70");
    }

    @Test
    @DisplayName("parar antes do alvo nao rende rotulo confirmatorio")
    void stopping_early_is_exploratory() {
        AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                true, AnalysisUnit.PAIR, StoppingRule.FIXED_SAMPLE_SIZE, 70, 48);

        assertThat(eligibility.kind()).isEqualTo(AnalysisKind.EXPLORATORY);
        assertThat(eligibility.justification()).contains("parou antes").contains("48").contains("70");
    }

    @Test
    @DisplayName("continuar coletando depois do alvo tambem e parada opcional")
    void collecting_past_the_target_is_exploratory_too() {
        AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                true, AnalysisUnit.PAIR, StoppingRule.FIXED_SAMPLE_SIZE, 70, 92);

        assertThat(eligibility.kind()).isEqualTo(AnalysisKind.EXPLORATORY);
        assertThat(eligibility.justification()).contains("parada opcional");
    }

    @Test
    @DisplayName("sem pre-registro congelado, nenhuma analise do estudo e confirmatoria")
    void without_a_frozen_preregistration_nothing_is_confirmatory() {
        AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                false, AnalysisUnit.PAIR, StoppingRule.FIXED_SAMPLE_SIZE, 70, 70);

        assertThat(eligibility.kind()).isEqualTo(AnalysisKind.EXPLORATORY);
        assertThat(eligibility.justification()).contains("congelado");
    }

    @Test
    @DisplayName("tratar as duas escolhas de um par como independentes e exploratorio")
    void forced_choice_as_unit_is_exploratory() {
        AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                true, AnalysisUnit.FORCED_CHOICE, StoppingRule.FIXED_SAMPLE_SIZE, 140, 140);

        assertThat(eligibility.kind()).isEqualTo(AnalysisKind.EXPLORATORY);
        assertThat(eligibility.justification()).contains("dependencia");
    }

    @Test
    @DisplayName("a justificativa nunca vem vazia, nem quando o veredito e favoravel")
    void the_justification_is_always_there() {
        for (int observed : new int[]{0, 69, 70, 71}) {
            AnalysisEligibility eligibility = ConfirmatoryAnalysisPolicy.evaluate(
                    true, AnalysisUnit.PAIR, StoppingRule.FIXED_SAMPLE_SIZE, 70, observed);
            assertThat(eligibility.justification()).isNotBlank();
        }
    }
}
