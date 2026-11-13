package io.github.rogeriocisi.airlab.notification;

import java.util.List;

/**
 * Sends e-mail, or keeps it, depending on the profile.
 *
 * <ul>
 *   <li><strong>local and test</strong>: nothing leaves the machine. Messages stay in an
 *       in-memory outbox, readable by the administrator, and are lost on restart.</li>
 *   <li><strong>prod with SMTP</strong> ({@code SPRING_MAIL_HOST} and friends): real e-mail.</li>
 *   <li><strong>prod without SMTP</strong>: disabled, and sign-up stays closed, because an
 *       account that cannot be confirmed is an account that cannot be used (E10, lock 3).</li>
 * </ul>
 */
public interface Mailer {

    /** Whether messages can be delivered, to an inbox or to the local outbox. */
    boolean enabled();

    /** Whether this is the local outbox rather than real e-mail. */
    boolean localOutbox();

    /**
     * Sends a message. A delivery failure is logged without the address and swallowed: an
     * e-mail problem must never undo the domain operation that triggered it.
     */
    void send(String to, String subject, String body);

    /** Messages kept by the local outbox, newest first; empty outside the local profile. */
    List<MailMessage> outbox();
}
