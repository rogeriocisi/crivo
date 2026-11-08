package io.github.rogeriocisi.airlab.analysis;

import io.github.rogeriocisi.airlab.study.AnalysisUnit;
import io.github.rogeriocisi.airlab.study.StoppingRule;

/**
 * Decides whether an analysis may be labelled confirmatory (Constitution P2, Epic E7).
 *
 * <p>This is the enforcement point for the preregistered sample size. Nothing can stop a
 * researcher from ending a study early, and the platform does not pretend otherwise: what it
 * can do is refuse to call the result confirmatory when the data does not match what was
 * declared. An analysis run on 48 units when 70 were preregistered is reported as
 * exploratory, with the reason printed next to it.
 *
 * <p>That answers "optional stopping", one of the standard criticisms of this literature:
 * stopping when the numbers look good no longer buys a confirmatory label.
 *
 * <p>Pure function on purpose — no clock, no database, no user. The same verdict comes out
 * of the verification package, in the hands of an auditor.
 */
public final class ConfirmatoryAnalysisPolicy {

    private ConfirmatoryAnalysisPolicy() {
    }

    /**
     * @param preregistrationFrozen whether the version being analysed was frozen before
     *                              collection started
     * @param declaredUnit          the unit of analysis in the frozen preregistration
     * @param stoppingRule          the stopping rule in the frozen preregistration
     * @param declaredSampleSize    the target number of independent units
     * @param observedUnits         independent units actually collected and locked
     */
    public static AnalysisEligibility evaluate(boolean preregistrationFrozen,
                                               AnalysisUnit declaredUnit,
                                               StoppingRule stoppingRule,
                                               int declaredSampleSize,
                                               int observedUnits) {
        if (!preregistrationFrozen) {
            return AnalysisEligibility.exploratory(
                    "O pre-registro nao estava congelado antes da coleta, portanto nenhuma "
                            + "analise deste estudo pode ser confirmatoria.");
        }
        if (declaredUnit != AnalysisUnit.PAIR) {
            return AnalysisEligibility.exploratory(
                    "A unidade de analise declarada (" + declaredUnit.name() + ") trata como "
                            + "independentes as duas escolhas de um mesmo par. A v1 nao implementa "
                            + "correcao para essa dependencia, portanto o resultado e exploratorio.");
        }
        if (stoppingRule != StoppingRule.FIXED_SAMPLE_SIZE) {
            return AnalysisEligibility.exploratory(
                    "Regra de parada declarada (" + stoppingRule.name() + ") nao suportada pela v1.");
        }
        if (observedUnits < declaredSampleSize) {
            return AnalysisEligibility.exploratory(
                    "A coleta parou antes do tamanho-alvo pre-registrado: " + observedUnits
                            + " de " + declaredSampleSize + " unidade(s) independente(s).");
        }
        if (observedUnits > declaredSampleSize) {
            return AnalysisEligibility.exploratory(
                    "A coleta passou do tamanho-alvo pre-registrado: " + observedUnits
                            + " de " + declaredSampleSize + " unidade(s) independente(s). Continuar "
                            + "coletando depois de atingir o alvo e uma forma de parada opcional.");
        }
        return AnalysisEligibility.confirmatory(
                "Analise confirmatoria: pre-registro congelado, unidade de analise por par e "
                        + observedUnits + " unidade(s) independente(s), exatamente o alvo declarado.");
    }
}
