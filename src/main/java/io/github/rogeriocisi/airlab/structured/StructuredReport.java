package io.github.rogeriocisi.airlab.structured;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The report of a P4 study (SPEC E13, phase D). It exists only after the unblinding — the database
 * refuses it before — so everything in it, the pair-to-medium link included, is already revealed.
 *
 * <p>Neutral by construction (P3): the same record and the same page serve a null result and a
 * positive one. {@link #primaryBelowAlpha()} is a statement about the test, not about the
 * phenomenon.
 *
 * @param document the canonical report, whose SHA-256 is in the audit trail
 */
public record StructuredReport(String studyCode,
                               int pairs,
                               BigDecimal primaryPValue,
                               String primaryMethod,
                               BigDecimal signPValue,
                               BigDecimal permutationPValue,
                               int sitterCommitmentsVerified,
                               int sitterCommitments,
                               int readingCommitmentsVerified,
                               int readingCommitments,
                               String seedHex,
                               String document,
                               String documentSha256,
                               Instant createdAt,
                               List<PairResult> pairResults) {

    /** The declared significance level of the one-sided primary test. */
    public static final BigDecimal ALPHA = new BigDecimal("0.05");

    public StructuredReport {
        pairResults = List.copyOf(pairResults);
    }

    public boolean primaryBelowAlpha() {
        return primaryPValue.compareTo(ALPHA) <= 0;
    }

    public boolean integrityHolds() {
        return sitterCommitmentsVerified == sitterCommitments && readingCommitmentsVerified == readingCommitments;
    }

    /** Pairs and sum of {@code d} per medium, to show whether one medium carries the result. */
    public List<MediumResult> mediumResults() {
        Map<String, int[]> byMedium = new TreeMap<>();
        for (PairResult pair : pairResults) {
            int[] totals = byMedium.computeIfAbsent(pair.mediumCode(), key -> new int[2]);
            totals[0]++;
            totals[1] += pair.d();
        }
        List<MediumResult> out = new ArrayList<>();
        byMedium.forEach((medium, totals) -> out.add(new MediumResult(medium, totals[0], totals[1])));
        return out;
    }

    /** One pair, as scored. */
    public record PairResult(String pairCode,
                             String mediumCode,
                             int comparableFields,
                             int targetMatchesA,
                             int decoyMatchesA,
                             int targetMatchesB,
                             int decoyMatchesB,
                             int d) {
    }

    public record MediumResult(String mediumCode, int pairs, int sumOfD) {
    }
}
