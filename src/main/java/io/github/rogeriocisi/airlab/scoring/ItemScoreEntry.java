package io.github.rogeriocisi.airlab.scoring;

/**
 * One item scored by a sitter (Epic E6).
 *
 * <p>Refuses to exist without the written explanation when the chosen value calls for one.
 * That is deliberate: an interpreted fit with no stated interpretation cannot be audited
 * later, and it is exactly the kind of score a critic would discount.
 *
 * @param justification what the sitter meant; required for an interpreted fit or a fit to
 *                      someone else, and must be absent for the other values, so that the
 *                      field cannot become a place for free commentary
 */
public record ItemScoreEntry(long itemId, ItemScoreValue value, String justification) {

    public ItemScoreEntry {
        boolean present = justification != null && !justification.isBlank();
        if (value.justificationRequired() && !present) {
            throw new IllegalArgumentException(
                    "o valor " + value.name() + " exige que o avaliador escreva a explicacao");
        }
        if (!value.justificationRequired() && present) {
            throw new IllegalArgumentException(
                    "o valor " + value.name() + " nao admite explicacao escrita");
        }
        justification = present ? justification.strip() : null;
    }

    public static ItemScoreEntry of(long itemId, ItemScoreValue value) {
        return new ItemScoreEntry(itemId, value, null);
    }
}
