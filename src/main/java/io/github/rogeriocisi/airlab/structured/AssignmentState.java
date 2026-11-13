package io.github.rogeriocisi.airlab.structured;

/**
 * Where a request to a medium stands. Says where it is, never how it scored: there is no score
 * before the unblinding (E10, lock 1).
 */
public enum AssignmentState {

    OPEN("Aguardando as leituras"),
    COMPLETED("Leituras enviadas"),
    EXPIRED("Prazo vencido — reenviado a outro médium"),
    CANCELLED("Cancelado");

    private final String label;

    AssignmentState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
