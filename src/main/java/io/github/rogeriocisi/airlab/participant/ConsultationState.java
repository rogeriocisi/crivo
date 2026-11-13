package io.github.rogeriocisi.airlab.participant;

/**
 * Where a P4 consultation request stands (SPEC E13, sitter panel). Phase B creates requests as
 * {@link #LOCKED}; phase C pairs them, sends them to a medium and records the readings; phase D
 * brings {@link #UNBLINDED}.
 *
 * <p>A state says where the request is, never how it scored: there is no score before
 * unblinding (E10, lock 1).
 */
public enum ConsultationState {

    LOCKED("Travado — aguardando par"),
    PAIRED("Par formado — aguardando médium"),
    SENT_TO_MEDIUM("Enviado ao médium"),
    READING_RECEIVED("Leitura recebida — aguardando o fim do estudo"),
    UNBLINDED("Estudo concluído — devolutiva disponível"),
    WITHDRAWN("Retirado");

    private final String label;

    ConsultationState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
