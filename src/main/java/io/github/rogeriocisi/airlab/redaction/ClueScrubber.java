package io.github.rogeriocisi.airlab.redaction;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Checks a redacted item list for the clues that would break the blinding (Epic E5, P1).
 *
 * <p>Inside a pair, the target's first name alone would reveal which reading is whose, so a
 * redaction containing it is rejected rather than cleaned silently: the redactor has to look
 * at the item again. Accents and case are ignored, because "Antonio" and "Antônio" are the
 * same clue.
 *
 * <p>This is a guard, not a guarantee. A reading can still leak through a rare detail no
 * rule can catch, which is why the matrix of SPEC section 4 also has tests and why incidents
 * are reportable.
 */
public final class ClueScrubber {

    /** Words that point at the session rather than at the deceased person. */
    private static final List<String> SESSION_CLUE_WORDS = List.of(
            "experimentador", "experimentadora", "sessao", "gravacao", "zoom", "whatsapp",
            "pesquisador", "pesquisadora", "medium", "transcricao");

    private ClueScrubber() {
    }

    /**
     * @param forbiddenNames first names of both targets of the pair
     * @return the problems found, empty when the redaction may be submitted
     */
    public static List<String> inspect(List<String> items, List<String> forbiddenNames) {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            String folded = fold(item);
            int ordinal = i + 1;

            if (item.isBlank()) {
                problems.add("Item " + ordinal + ": vazio.");
                continue;
            }
            for (String name : forbiddenNames) {
                if (name != null && !name.isBlank() && containsWord(folded, fold(name))) {
                    problems.add("Item " + ordinal + ": contem o primeiro nome de um dos alvos.");
                }
            }
            for (String clue : SESSION_CLUE_WORDS) {
                if (containsWord(folded, clue)) {
                    problems.add("Item " + ordinal + ": contem uma pista de sessao (\"" + clue + "\").");
                }
            }
            if (hasDateLikeToken(folded)) {
                problems.add("Item " + ordinal + ": parece conter uma data de sessao.");
            }
            if (countSentences(item) > 1) {
                problems.add("Item " + ordinal + ": parece conter mais de uma afirmacao; "
                        + "um item = uma afirmacao verificavel.");
            }
        }
        return problems;
    }

    private static boolean containsWord(String foldedText, String foldedWord) {
        int from = 0;
        while (true) {
            int at = foldedText.indexOf(foldedWord, from);
            if (at < 0) {
                return false;
            }
            boolean leftOk = at == 0 || !Character.isLetterOrDigit(foldedText.charAt(at - 1));
            int end = at + foldedWord.length();
            boolean rightOk = end >= foldedText.length() || !Character.isLetterOrDigit(foldedText.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = at + 1;
        }
    }

    /** Looks for 4 digit years and day/month patterns without leaning on regex escapes. */
    private static boolean hasDateLikeToken(String text) {
        int digitRun = 0;
        int separators = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                digitRun++;
                if (digitRun >= 4) {
                    return true;
                }
            } else {
                if ((c == '/' || c == '-' || c == '.') && digitRun > 0) {
                    separators++;
                    if (separators >= 2) {
                        return true;
                    }
                } else if (c != ' ') {
                    separators = 0;
                }
                digitRun = 0;
            }
        }
        return false;
    }

    private static int countSentences(String item) {
        int count = 0;
        boolean inSentence = false;
        for (int i = 0; i < item.length(); i++) {
            char c = item.charAt(i);
            if (c == '.' || c == ';' || c == '!' || c == '?') {
                if (inSentence) {
                    count++;
                    inSentence = false;
                }
            } else if (!Character.isWhitespace(c)) {
                inSentence = true;
            }
        }
        return inSentence ? count + 1 : count;
    }

    private static String fold(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                out.append(c);
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
