package io.github.rogeriocisi.airlab.notification.internal;

import io.github.rogeriocisi.airlab.notification.Mailer;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.structured.ReadingRequestSent;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells the drawn medium that a request arrived (SPEC E13: "o médium recebe e-mail quando chega um
 * pedido"). Sent only after the cycle commits, so nobody is told about a request that a rollback
 * undid.
 *
 * <p>No first name in the message, and no pair code: e-mail is not a blind channel, and the names
 * are one sign-in away, on the panel.
 */
@Component
class MediumRequestMail {

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final Mailer mailer;
    private final AccountService accounts;
    private final String baseUrl;

    MediumRequestMail(Mailer mailer, AccountService accounts, Environment environment) {
        this.mailer = mailer;
        this.accounts = accounts;
        this.baseUrl = environment.getProperty("airlab.base-url", "http://localhost:8080").replaceAll("/+$", "");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    void on(ReadingRequestSent event) {
        if (!mailer.enabled()) {
            return;
        }
        accounts.mailAddress(event.mediumCode()).ifPresent(address -> mailer.send(address,
                "Crivo: um pedido de leitura para você",
                """
                Olá.

                Há um pedido de leitura para você no Crivo: dois primeiros nomes e o questionário em branco.

                Prazo para enviar as duas leituras: %s (UTC).

                Entre no seu painel para ver os nomes e responder:
                %s/painel/medium

                Este e-mail não traz os nomes de propósito: e-mail não é um canal cego.
                """.formatted(DUE.format(event.dueAt()), baseUrl)));
    }
}
