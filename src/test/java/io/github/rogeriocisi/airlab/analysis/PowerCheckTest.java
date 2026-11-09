package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The numbers that justify making the paired measure the primary one (SPEC section 9).
 *
 * <p>The reference values are the standard one-sided formulas at alpha 0.05 and power 0.80,
 * computed independently; they are the table in {@code docs/PLAN.md}.
 */
class PowerCheckTest {

    @ParameterizedTest(name = "escolha forcada a {0} exige {1} pares")
    @CsvSource({"0.56, 428", "0.58, 240", "0.60, 153", "0.65, 67"})
    @DisplayName("a escolha forcada exige amostras grandes para os efeitos da meta-analise")
    void forced_choice_requirements(double hitRate, int expected) {
        assertThat(PowerCheck.requiredPairs(PrimaryMeasure.FORCED_CHOICE, hitRate))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "medida pareada com d={0} exige {1} pares")
    @CsvSource({"0.49, 30", "0.50, 29", "0.57, 22", "0.75, 13"})
    @DisplayName("a medida pareada continua exige uma fracao disso")
    void paired_requirements(double d, int expected) {
        assertThat(PowerCheck.requiredPairs(PrimaryMeasure.PAIRED_ITEM_ACCURACY, d))
                .isEqualTo(expected);
        assertThat(PowerCheck.requiredPairs(PrimaryMeasure.PAIRED_GLOBAL_SCORE, d))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("as 70 unidades da SPEC sao insuficientes para o efeito da meta-analise")
    void the_original_seventy_units_are_not_enough() {
        PowerAssessment assessment =
                PowerCheck.assess(PrimaryMeasure.FORCED_CHOICE, 0.58, 70);

        assertThat(assessment.adequate()).isFalse();
        assertThat(assessment.requiredPairs()).isEqualTo(240);
        assertThat(assessment.detail())
                .contains("abaixo do minimo")
                .contains("nao distinguira");
    }

    @Test
    @DisplayName("com a medida pareada o mesmo estudo fica dimensionado")
    void the_paired_measure_makes_the_same_study_adequate() {
        PowerAssessment assessment =
                PowerCheck.assess(PrimaryMeasure.PAIRED_GLOBAL_SCORE, 0.5, 30);

        assertThat(assessment.adequate()).isTrue();
        assertThat(assessment.requiredPairs()).isEqualTo(29);
        assertThat(assessment.detail()).contains("atende o minimo");
    }

    @Test
    @DisplayName("uma taxa de acerto que nao supera o acaso nao tem calculo unilateral")
    void a_hit_rate_at_chance_is_refused() {
        assertThatThrownBy(() -> PowerCheck.requiredPairs(PrimaryMeasure.FORCED_CHOICE, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
