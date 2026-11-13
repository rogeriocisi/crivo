package io.github.rogeriocisi.airlab.participant.internal;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The two linked-account signals (SPEC E13, phase E), before they go into the blind index. Pure
 * functions; the canonical forms never leave this module and are never stored in clear.
 */
final class AccountSignals {

    private static final Set<String> GMAIL = Set.of("gmail.com", "googlemail.com");

    private AccountSignals() {
    }

    /**
     * The mailbox behind an address: lower case, without a "+tag", and for Gmail without the dots,
     * which Gmail ignores — so {@code Jo.Ao+crivo@GoogleMail.com} and {@code joao@gmail.com} meet.
     */
    static String canonicalEmail(String email) {
        String address = email.strip().toLowerCase(Locale.ROOT);
        int at = address.lastIndexOf('@');
        if (at <= 0) {
            return address;
        }
        String local = address.substring(0, at);
        String domain = address.substring(at + 1);
        int plus = local.indexOf('+');
        if (plus > 0) {
            local = local.substring(0, plus);
        }
        if (GMAIL.contains(domain)) {
            local = local.replace(".", "");
            domain = "gmail.com";
        }
        return local + "@" + domain;
    }

    /**
     * A full name compared without case, accents, punctuation or extra spaces. Empty for a single
     * word: "Maria" alone matches far too many people to mean anything.
     */
    static Optional<String> normalizedName(String fullName) {
        String plain = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\s]", " ")
                .strip()
                .replaceAll("\\s+", " ");
        return plain.contains(" ") ? Optional.of(plain) : Optional.empty();
    }
}
