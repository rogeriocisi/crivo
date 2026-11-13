package io.github.rogeriocisi.airlab.structured;

import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The P4 scoring rule (SPEC E13, "Conferência automática"), as a public pure function so that the
 * verification package can run exactly this code — or reimplement it — and get the same {@code d}.
 *
 * <ul>
 *   <li><strong>Comparable fields</strong> of a pair: the scored fields that <em>both</em> sitters
 *       answered (neither marked "não sei"). The same set serves the target and the decoy, so
 *       neither has more chances to match.</li>
 *   <li>{@code s(R, X)}: comparable fields on which the reading {@code R} equals the answer of
 *       sitter {@code X}. "Não recebi" never matches.</li>
 *   <li>{@code d = [s(R_A, A) - s(R_A, B)] + [s(R_B, B) - s(R_B, A)]}.</li>
 * </ul>
 *
 * <p>Equality between closed options, nothing else: no judgement, no free text, no model (P10).
 */
public final class StructuredScoring {

    private StructuredScoring() {
    }

    /**
     * The score of a pair.
     *
     * @param comparableFields the field keys both sitters answered, in questionnaire order
     * @param targetMatchesA   {@code s(R_A, A)}
     * @param decoyMatchesA    {@code s(R_A, B)}
     * @param targetMatchesB   {@code s(R_B, B)}
     * @param decoyMatchesB    {@code s(R_B, A)}
     */
    public record PairScore(List<String> comparableFields,
                            int targetMatchesA,
                            int decoyMatchesA,
                            int targetMatchesB,
                            int decoyMatchesB) {

        public PairScore {
            comparableFields = List.copyOf(comparableFields);
        }

        public int d() {
            return (targetMatchesA - decoyMatchesA) + (targetMatchesB - decoyMatchesB);
        }
    }

    public static PairScore score(Questionnaire questionnaire, Map<String, String> sitterA, Map<String, String> sitterB,
                                  Map<String, String> readingA, Map<String, String> readingB) {
        List<String> fields = comparableFields(questionnaire, sitterA, sitterB);
        return new PairScore(fields,
                matches(fields, readingA, sitterA),
                matches(fields, readingA, sitterB),
                matches(fields, readingB, sitterB),
                matches(fields, readingB, sitterA));
    }

    /** Scored fields where neither sitter answered "não sei", in questionnaire order. */
    public static List<String> comparableFields(Questionnaire questionnaire, Map<String, String> sitterA,
                                                Map<String, String> sitterB) {
        List<String> fields = new ArrayList<>();
        for (Questionnaire.Field field : questionnaire.scoredFields()) {
            if (answered(sitterA.get(field.key())) && answered(sitterB.get(field.key()))) {
                fields.add(field.key());
            }
        }
        return fields;
    }

    /** How many of the fields the reading answers exactly as the sitter did. */
    public static int matches(List<String> fields, Map<String, String> reading, Map<String, String> sitter) {
        int matches = 0;
        for (String field : fields) {
            String read = reading.get(field);
            if (read != null && !Questionnaire.NOT_RECEIVED.equals(read) && read.equals(sitter.get(field))) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * The cell of the permutation matrix (SPEC E13, phase D): matches between a reading and a sitter
     * over every scored field that sitter answered. It does not depend on who the decoy was, which
     * is what lets any reading be set against any target of its stratum.
     */
    public static int answeredMatches(Questionnaire questionnaire, Map<String, String> reading,
                                      Map<String, String> sitter) {
        List<String> answeredBySitter = new ArrayList<>();
        for (Questionnaire.Field field : questionnaire.scoredFields()) {
            if (answered(sitter.get(field.key()))) {
                answeredBySitter.add(field.key());
            }
        }
        return matches(answeredBySitter, reading, sitter);
    }

    private static boolean answered(String answer) {
        return answer != null && !answer.isBlank() && !Questionnaire.UNKNOWN.equals(answer);
    }
}
