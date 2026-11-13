package io.github.rogeriocisi.airlab.notification;

import java.time.Instant;

/**
 * One e-mail. Plain text only: no tracking pixel, no remote image, nothing that reports back
 * when the message is opened.
 *
 * @param to      recipient address
 * @param subject subject line, which must never carry personal data
 * @param body    plain text body
 * @param at      when it was handed to the mailer
 */
public record MailMessage(String to, String subject, String body, Instant at) {

    @Override
    public String toString() {
        // The address and the body stay out of any log (P6).
        return "MailMessage[subject=" + subject + ", at=" + at + "]";
    }
}
