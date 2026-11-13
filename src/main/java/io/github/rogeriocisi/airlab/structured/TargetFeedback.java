package io.github.rogeriocisi.airlab.structured;

import java.util.List;
import java.util.Map;

/**
 * What a sitter gets after the unblinding (SPEC E13, phase D): the medium's reading of their own
 * target, field by field, which fields counted, and two counts — matches with their own answers
 * and, for comparison, with the other sitter of the pair.
 *
 * <p>Never the other sitter's answers, and no medium code: the count against the decoy is all the
 * comparison needs.
 *
 * @param readingAnswers   the medium's answers for this sitter's target, by field key
 * @param comparableFields the field keys both sitters of the pair answered
 * @param targetMatches    comparable fields where the reading equals this sitter's answer
 * @param decoyMatches     comparable fields where the same reading equals the other sitter's answer
 */
public record TargetFeedback(String studyCode,
                             String questionnaireVersion,
                             Map<String, String> readingAnswers,
                             List<String> comparableFields,
                             int targetMatches,
                             int decoyMatches) {

    public TargetFeedback {
        readingAnswers = Map.copyOf(readingAnswers);
        comparableFields = List.copyOf(comparableFields);
    }

    @Override
    public String toString() {
        return "TargetFeedback[study=" + studyCode + ", redacted]";
    }
}
