package io.github.rogeriocisi.airlab.notification;

/**
 * Completed steps the administrator is told about (SPEC E13, "Avisos ao administrador").
 * Phase A emits only {@link #ACCOUNT_CONFIRMED}; the others arrive with phases B to D and are
 * listed now so the panel and the SPEC say the same thing.
 */
public enum NoticeKind {

    ACCOUNT_CONFIRMED("Conta confirmada"),
    REQUEST_LOCKED("Pedido de consulta travado"),
    PAIR_SENT_TO_MEDIUM("Par formado e enviado ao médium"),
    READING_RECEIVED("Leitura recebida"),
    READING_OVERDUE("Prazo de leitura vencido; pedido reenviado"),
    ACCOUNTS_FLAGGED("Contas ligadas sinalizadas para revisão"),
    STUDY_UNBLINDED("Número de pares atingido; estudo desmascarado"),
    REPORT_READY("Relatório pronto");

    private final String label;

    NoticeKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
