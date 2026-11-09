package io.github.rogeriocisi.airlab.analysis;

/**
 * How far the collection has got — and nothing else (Epic E10, lock 1).
 *
 * <p>This record exists to be the <em>only</em> thing a researcher can see about a running
 * study before the unblinding. It says how many independent units are in and how many are
 * missing; it carries no hit count, no proportion, no difference between target and decoy, no
 * p value, and no field from which any of those could be derived.
 *
 * <p>The reason is the whole point of the preregistration: a progress bar that leaks the result
 * turns every day of collection into a decision about whether to stop, which is the optional
 * stopping that {@link ConfirmatoryAnalysisPolicy} refuses to reward. Automating the study
 * without this lock would make the problem worse than it is on paper, because the number would
 * be in front of the researcher continuously rather than at the end.
 *
 * <p>A structural test asserts that no component of this record is named after an outcome, so
 * that adding one later fails the build rather than slipping through review.
 *
 * @param collectedUnits independent units already locked
 * @param declaredUnits  the target declared in the frozen preregistration
 */
public record CollectionProgress(long studyId, int collectedUnits, int declaredUnits) {

    public CollectionProgress {
        if (collectedUnits < 0 || declaredUnits < 1) {
            throw new IllegalArgumentException("counts must be non-negative and the target positive");
        }
    }

    public int remainingUnits() {
        return Math.max(0, declaredUnits - collectedUnits);
    }

    /** True when the declared target is reached and the analysis may run. */
    public boolean targetReached() {
        return collectedUnits >= declaredUnits;
    }

    /**
     * Sentence for the researcher's screen. Deliberately dull: it reports counts, never how the
     * study is "going".
     */
    public String summary() {
        if (targetReached()) {
            return "Coleta completa: " + collectedUnits + " de " + declaredUnits
                    + " unidade(s) independente(s). A analise pre-registrada pode ser executada.";
        }
        return "Coleta em andamento: " + collectedUnits + " de " + declaredUnits
                + " unidade(s) independente(s), faltam " + remainingUnits()
                + ". Nenhum resultado e calculado ou exibido antes do desmascaramento.";
    }
}
