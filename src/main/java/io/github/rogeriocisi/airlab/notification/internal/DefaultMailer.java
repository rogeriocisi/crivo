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
 *
 * <p>In the local profile the in-memory outbox is the default; setting {@code SPRING_MAIL_HOST}
 * there opts into real SMTP, meant for a local catcher such as Mailpit, which shows the messages
 * in a web inbox and lets none of them leave the machine. The test profile always keeps the
 * outbox, whatever the environment says.
 */
@Component
class DefaultMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(DefaultMailer.class);

    private static final int OUTBOX_SIZE = 50;

    enum Mode { LOCAL, SMTP, DISABLED }

    private final Mode mode;
    private final JavaMailSender smtp;
    private final String from;
    private final Clock clock;
    private final Deque<MailMessage> outbox = new ArrayDeque<>();

    DefaultMailer(Environment environment, ObjectProvider<JavaMailSender> smtp, Clock clock) {
        this.smtp = smtp.getIfAvailable();
        this.from = environment.getProperty("airlab.mail-from", "crivo@localhost");
        this.clock = clock;
        this.mode = modeFor(environment.matchesProfiles("test"), environment.matchesProfiles("local"),
                this.smtp != null);
        switch (mode) {
            case SMTP -> {
                if (environment.matchesProfiles("local")) {
                    log.info("Perfil local com SMTP: e-mails vao para {}:{} (por exemplo, o Mailpit)",
                            environment.getProperty("spring.mail.host"), environment.getProperty("spring.mail.port", "25"));
                }
            }
            case DISABLED -> log.warn("Envio de e-mail desativado: SPRING_MAIL_HOST nao definido. "
                    + "A inscricao de contas fica fechada ate que um SMTP seja configurado.");
            case LOCAL -> {
            }
        }
    }

    /**
     * Tests keep the outbox, always. The local profile keeps it unless an SMTP server was
     * configured on purpose. Anywhere else, SMTP or nothing.
     */
    static Mode modeFor(boolean testProfile, boolean localProfile, boolean smtpConfigured) {
        if (testProfile || (localProfile && !smtpConfigured)) {
            return Mode.LOCAL;
        }
        return smtpConfigured ? Mode.SMTP : Mode.DISABLED;
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
