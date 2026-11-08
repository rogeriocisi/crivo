package io.github.rogeriocisi.airlab.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reduces the collected forced choices to the independent observations of the confirmatory
 * analysis (Epic E7, SPEC section 9, question 1).
 *
 * <p>Both sitters of a pair judge the same two readings, mirrored, so their choices are not
 * independent. Only the choice of the sitter the draw designated for the pair enters the
 * confirmatory count; the other one is kept and reported as a secondary analysis.
 *
 * <p>Pure function, and deliberately strict: a pair with no designated sitter, a designated
 * sitter who did not submit, or a duplicated choice makes it throw rather than quietly
 * change the denominator of the primary analysis.
 */
public final class IndependentUnits {

    private IndependentUnits() {
    }

    /**
     * One submitted forced choice.
     *
     * @param sitterTargetCode the target of the sitter who made the choice, which identifies
     *                         the sitter without naming them
     * @param chosenOwnReading whether the sitter picked the reading of their own target
     */
    public record Choice(String pairCode, String sitterTargetCode, boolean chosenOwnReading) {
    }

    /** Hits and trials of the confirmatory analysis. */
    public record Counts(int hits, int trials) {
    }

    /**
     * @param confirmatoryTargetByPair pair code to designated target code, from the drawn
     *                                 allocation plan
     */
    public static Counts confirmatoryCounts(List<Choice> choices,
                                            Map<String, String> confirmatoryTargetByPair) {
        List<Choice> designated = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();

        for (Choice choice : choices) {
            String designatedTarget = confirmatoryTargetByPair.get(choice.pairCode());
            if (designatedTarget == null) {
                throw new IllegalArgumentException(
                        "pair " + choice.pairCode() + " has no designated confirmatory unit");
            }
            if (!designatedTarget.equals(choice.sitterTargetCode())) {
                continue;
            }
            if (!seenPairs.add(choice.pairCode())) {
                throw new IllegalArgumentException(
                        "more than one confirmatory choice for pair " + choice.pairCode());
            }
            designated.add(choice);
        }

        for (String pairCode : confirmatoryTargetByPair.keySet()) {
            if (!seenPairs.contains(pairCode)) {
                throw new IllegalArgumentException(
                        "pair " + pairCode + " has no choice from its designated sitter; "
                                + "exclude the pair by the preregistered rule instead of "
                                + "silently shrinking the sample");
            }
        }

        designated.sort(Comparator.comparing(Choice::pairCode));
        int hits = 0;
        for (Choice choice : designated) {
            if (choice.chosenOwnReading()) {
                hits++;
            }
        }
        return new Counts(hits, designated.size());
    }
}
