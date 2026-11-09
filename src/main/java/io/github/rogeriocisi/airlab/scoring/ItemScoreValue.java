package io.github.rogeriocisi.airlab.scoring;

/**
 * The preregistered item scale (Epic E6).
 *
 * <p>Adapted from the AMPSS scale of Beischel and Schwartz. Two of these categories are the
 * reason the scale is not simply right/partly/wrong:
 *
 * <ul>
 *   <li>{@link #FITS_SOMEONE_ELSE} catches the item that fits a person close to the sitter but
 *       not the target. Without it, the sitter is forced to call that item a hit or a miss, and
 *       either way the score hides the alternative explanation.</li>
 *   <li>{@link #PARTIAL_OR_INTERPRETED_FIT} is where cold reading lives: an item that only fits
 *       once the reader works at it. Hyman's criticism is precisely that the way accuracy is
 *       scored can manufacture significance, so this category exists to be counted separately
 *       and to require the sitter to write down the interpretation.</li>
 * </ul>
 *
 * <p>The wording the sitter sees stays neutral (P3): the scale is about whether a statement
 * fits a person, never about whether a communication took place.
 */
public enum ItemScoreValue {

    /** Fits the target clearly, with no interpretation needed. */
    OBVIOUS_FIT(false),

    /** Fits only partly, or only under an interpretation the sitter must state. */
    PARTIAL_OR_INTERPRETED_FIT(true),

    /** Does not fit the target. */
    NO_FIT(false),

    /** Fits someone else the sitter was close to, but not the target. Must be explained. */
    FITS_SOMEONE_ELSE(true),

    /** The sitter has no way to tell. Counted, never silently treated as a miss. */
    CANNOT_ASSESS(false);

    private final boolean justificationRequired;

    ItemScoreValue(boolean justificationRequired) {
        this.justificationRequired = justificationRequired;
    }

    /** Whether the sitter must write what they meant when choosing this value. */
    public boolean justificationRequired() {
        return justificationRequired;
    }

    /** Whether this value counts towards accuracy in the preregistered primary measure. */
    public boolean countsAsFit() {
        return this == OBVIOUS_FIT;
    }
}
