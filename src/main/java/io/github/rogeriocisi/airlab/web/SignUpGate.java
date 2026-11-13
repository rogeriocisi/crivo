package io.github.rogeriocisi.airlab.web;

import io.github.rogeriocisi.airlab.notification.Mailer;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Whether P4 sign-up is open on this installation (SPEC E13).
 *
 * <p>Open only when an account can actually be confirmed and a person can be reached: e-mail
 * working (E10, lock 3) and, outside the local profile, a human contact configured (E10,
 * lock 5). A closed sign-up says why, in the page, instead of failing half-way.
 */
@Component
class SignUpGate {

    private final Mailer mailer;
    private final Environment environment;

    SignUpGate(Mailer mailer, Environment environment) {
        this.mailer = mailer;
        this.environment = environment;
    }

    boolean open() {
        return mailer.enabled() && (localProfile() || contactEmail().isPresent());
    }

    boolean localProfile() {
        return environment.matchesProfiles("local", "test");
    }

    /**
     * Whether sitters may file P4 requests: the request collects sensitive data about a deceased
     * person, so outside the local profile it needs the ethics committee approval reference
     * (E10, lock 4) on top of an open sign-up.
     */
    boolean requestsOpen() {
        return open() && (localProfile() || cepReference().isPresent());
    }

    Optional<String> cepReference() {
        String reference = environment.getProperty("airlab.p4.cep-reference", "").strip();
        return reference.isEmpty() ? Optional.empty() : Optional.of(reference);
    }

    Optional<String> contactEmail() {
        String contact = environment.getProperty("airlab.contact-email", "").strip();
        return contact.isEmpty() ? Optional.empty() : Optional.of(contact);
    }
}
