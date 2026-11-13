package io.github.rogeriocisi.airlab.notification.internal;

import io.github.rogeriocisi.airlab.notification.MailMessage;
import io.github.rogeriocisi.airlab.notification.Mailer;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * The one mailer. The mode is decided once, at start up, from the profile and from whether
 * Spring Boot configured an SMTP sender ({@code spring.mail.host}).
 */
@Component
class DefaultMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(DefaultMailer.class);

    private static final int OUTBOX_SIZE = 50;

    private enum Mode { LOCAL, SMTP, DISABLED }

    private final Mode mode;
    private final JavaMailSender smtp;
    private final String from;
    private final Clock clock;
    private final Deque<MailMessage> outbox = new ArrayDeque<>();

    DefaultMailer(Environment environment, ObjectProvider<JavaMailSender> smtp, Clock clock) {
        this.smtp = smtp.getIfAvailable();
        this.from = environment.getProperty("airlab.mail-from", "crivo@localhost");
        this.clock = clock;
        if (environment.matchesProfiles("local", "test")) {
            this.mode = Mode.LOCAL;
        } else if (this.smtp != null) {
            this.mode = Mode.SMTP;
        } else {
            this.mode = Mode.DISABLED;
            log.warn("Envio de e-mail desativado: SPRING_MAIL_HOST nao definido. "
                    + "A inscricao de contas fica fechada ate que um SMTP seja configurado.");
        }
    }

    @Override
    public boolean enabled() {
        return mode != Mode.DISABLED;
    }

    @Override
    public boolean localOutbox() {
        return mode == Mode.LOCAL;
    }

    @Override
    public void send(String to, String subject, String body) {
        MailMessage message = new MailMessage(to, subject, body, clock.instant());
        switch (mode) {
            case LOCAL -> {
                synchronized (outbox) {
                    outbox.addFirst(message);
                    while (outbox.size() > OUTBOX_SIZE) {
                        outbox.removeLast();
                    }
                }
                log.info("Perfil local: e-mail retido na caixa de saida, sem envio (assunto: {})", subject);
            }
            case SMTP -> {
                try {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setFrom(from);
                    mail.setTo(to);
                    mail.setSubject(subject);
                    mail.setText(body);
                    smtp.send(mail);
                } catch (RuntimeException e) {
                    // No address in the log (P6); the exception type is enough to start looking.
                    log.warn("Falha ao enviar e-mail (assunto: {}): {}", subject, e.getClass().getSimpleName());
                }
            }
            case DISABLED -> log.warn("E-mail descartado: envio desativado (assunto: {})", subject);
        }
    }

    @Override
    public List<MailMessage> outbox() {
        if (mode != Mode.LOCAL) {
            return List.of();
        }
        synchronized (outbox) {
            return List.copyOf(outbox);
        }
    }
}
