package io.github.rogeriocisi.airlab.structured;

/**
 * What a medium gets about one of their readings after the unblinding (SPEC E13, phase D): counts
 * only. The sitters' answers are theirs, and the medium never sees them.
 *
 * @param targetMatches  comparable fields where the reading matched the sitter it was made for
 * @param decoyMatches   comparable fields where it matched the other sitter of the pair
 * @param pairDifference {@code d} of the pair, both readings counted
 */
public record ReadingFeedback(String studyCode,
                              String assignmentCode,
                              int position,
                              String firstName,
                              int comparableFields,
                              int targetMatches,
                              int decoyMatches,
                              int pairDifference) {

    @Override
    public String toString() {
        return "ReadingFeedback[assignment=" + assignmentCode + ", position=" + position + ", redacted]";
    }
}
