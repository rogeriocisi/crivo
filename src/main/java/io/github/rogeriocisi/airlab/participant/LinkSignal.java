package io.github.rogeriocisi.airlab.participant;

/**
 * What two accounts were found to share (SPEC E13, phase E). Both are compared through a blind
 * index; neither the address nor the name is stored in clear for this.
 */
public enum LinkSignal {

    /** The same mailbox behind two addresses: case, a "+tag", or dots in a Gmail address. */
    EMAIL_CANONICAL("Mesmo e-mail, com outra grafia"),

    /** The same full name, ignoring case, accents and spacing; two words at least. */
    FULL_NAME("Mesmo nome completo");

    private final String label;

    LinkSignal(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
