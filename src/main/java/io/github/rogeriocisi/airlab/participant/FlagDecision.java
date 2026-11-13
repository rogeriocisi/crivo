package io.github.rogeriocisi.airlab.participant;

/** The administrator's review of a linked-accounts flag (SPEC E13, phase E). Made once. */
public enum FlagDecision {

    /** The same person: the two accounts keep apart in every pair, for good. */
    LINKED("Mesma pessoa"),

    /** No relation: the restriction falls. */
    UNRELATED("Sem relação");

    private final String label;

    FlagDecision(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
