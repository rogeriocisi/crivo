package io.github.rogeriocisi.airlab.participant;

/**
 * Profile of a P4 account (SPEC E13). Chosen once, at sign-up, and never changed: the database
 * refuses the change too (V17).
 *
 * <p>There is no administrator here on purpose. The administrator comes from the server
 * configuration, and no path of the site can create one.
 */
public enum AccountProfile {

    SITTER("Consulente", "CON", ParticipantRole.SITTER),
    MEDIUM("Médium", "MED", ParticipantRole.MEDIUM);

    private final String label;
    private final String codePrefix;
    private final ParticipantRole role;

    AccountProfile(String label, String codePrefix, ParticipantRole role) {
        this.label = label;
        this.codePrefix = codePrefix;
        this.role = role;
    }

    /** Name shown to people, in Portuguese. */
    public String label() {
        return label;
    }

    /** Prefix of the pseudonymous code, so a code says which side it is on. */
    public String codePrefix() {
        return codePrefix;
    }

    /** The role this profile holds in a study. */
    public ParticipantRole role() {
        return role;
    }
}
