package io.github.rogeriocisi.airlab.analysis;

/**
 * What the declared design can and cannot detect, written into the frozen preregistration.
 *
 * @param expectedEffect expected hit rate for the forced choice, expected d for the paired
 *                       measures
 * @param adequate       whether the declared number of pairs reaches the requirement
 * @param detail         the sentence that goes into the document and into the report
 */
public record PowerAssessment(PrimaryMeasure measure,
                              double expectedEffect,
                              int declaredPairs,
                              int requiredPairs,
                              boolean adequate,
                              String detail) {
}
