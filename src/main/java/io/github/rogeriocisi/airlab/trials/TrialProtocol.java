package io.github.rogeriocisi.airlab.trials;

/**
 * The self-service protocols of the catalog: the ones that run end to end with nobody in the
 * loop (SPEC, section 1.1, codes P2 and P3).
 *
 * <p>The paired reading (P1) is not here, and that is the point: it needs a synchronous
 * session with a proxy experimenter, so it is not a trial protocol.
 */
public enum TrialProtocol {

    /** Four abstract alternatives, target derived after the answer is locked. */
    PRECOGNITIVE_FORCED_CHOICE("P2", "escolha-forcada", 4, 24, false),

    /** Four figures from the versioned pool, ranked after the impression is locked. */
    REMOTE_VIEWING_RANKED("P3", "visao-remota", 4, 6, true);

    private final String catalogCode;
    private final String slug;
    private final int alternatives;
    private final int defaultTrials;
    private final boolean impressionRequired;

    TrialProtocol(String catalogCode, String slug, int alternatives, int defaultTrials,
                  boolean impressionRequired) {
        this.catalogCode = catalogCode;
        this.slug = slug;
        this.alternatives = alternatives;
        this.defaultTrials = defaultTrials;
        this.impressionRequired = impressionRequired;
    }

    public String catalogCode() {
        return catalogCode;
    }

    public String slug() {
        return slug;
    }

    /** Number of alternatives per trial, which fixes the declared chance level at 1/this. */
    public int alternatives() {
        return alternatives;
    }

    /**
     * Trials per session when the person does not choose another number. Fewer for remote
     * viewing, because writing an impression takes minutes and an abandoned session still
     * counts in the denominator.
     */
    public int defaultTrials() {
        return defaultTrials;
    }

    /** Whether a written impression must be locked before the candidates are shown (E12). */
    public boolean impressionRequired() {
        return impressionRequired;
    }
}
