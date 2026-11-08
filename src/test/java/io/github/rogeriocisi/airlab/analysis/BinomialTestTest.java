package io.github.rogeriocisi.airlab.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Constitution P9: statistical functions are validated against known reference values.
 *
 * <p>The references below are exact: each one is the rational number
 * {@code sum(C(n,k), k >= hits) / 2^n}, computed independently with exact integer arithmetic,
 * and they match {@code binom.test(hits, n, 0.5, alternative = "greater")} in R.
 */
class BinomialTestTest {

    @ParameterizedTest(name = "p({0}/{1}) = {2}")
    @CsvSource({
            "46, 70, 0.00576320482718174071",
            "40, 70, 0.14098946089682803939",
            "35, 70, 0.54751273677026879305",
            "58, 100, 0.06660530960360666608",
            "10, 10, 0.00097656250000000000",
            "7, 12, 0.38720703125000000000",
            "3, 5, 0.50000000000000000000",
            "0, 5, 1.00000000000000000000"
    })
    @DisplayName("o p-valor exato confere com os valores de referencia")
    void matches_reference_values(int hits, int trials, String expected) {
        BigDecimal actual = BinomialTest.pValueGreaterOrEqual(hits, trials);

        assertThat(actual.doubleValue())
                .isCloseTo(Double.parseDouble(expected), org.assertj.core.data.Offset.offset(1e-15));
    }

    @Test
    @DisplayName("o resultado primario traz proporcao, p-valor e a comparacao com alfa")
    void primary_analysis_reports_everything_the_report_needs() {
        PrimaryAnalysis analysis = BinomialTest.primary(46, 70, new BigDecimal("0.05"));

        assertThat(analysis.hits()).isEqualTo(46);
        assertThat(analysis.trials()).isEqualTo(70);
        assertThat(analysis.proportion().doubleValue()).isCloseTo(0.657142857,
                org.assertj.core.data.Offset.offset(1e-9));
        assertThat(analysis.belowAlpha()).isTrue();
    }

    @Test
    @DisplayName("um resultado nulo e relatado do mesmo jeito, sem destaque diferente (P3)")
    void a_null_result_is_just_a_result() {
        PrimaryAnalysis analysis = BinomialTest.primary(35, 70, new BigDecimal("0.05"));

        assertThat(analysis.belowAlpha()).isFalse();
        assertThat(analysis.pValue().doubleValue()).isGreaterThan(0.5);
    }

    @Test
    @DisplayName("contagens impossiveis sao recusadas, nao arredondadas")
    void rejects_impossible_counts() {
        assertThatThrownBy(() -> BinomialTest.pValueGreaterOrEqual(8, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
