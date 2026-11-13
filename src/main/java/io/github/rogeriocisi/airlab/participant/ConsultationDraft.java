package io.github.rogeriocisi.airlab.participant;

import java.util.Map;
import java.util.TreeMap;

/**
 * What the sitter fills in before locking a P4 request: the deceased person's first name and
 * the answers to the questionnaire, by field key. Kept only in the browser session until the
 * sitter confirms; nothing is stored before the lock.
 */
public record ConsultationDraft(String firstName, Map<String, String> answers) implements java.io.Serializable {

    public ConsultationDraft {
        answers = answers == null ? Map.of() : java.util.Collections.unmodifiableMap(new TreeMap<>(answers));
    }

    @Override
    public String toString() {
        // The name and the answers are sensitive (P6): never in a log or a message.
        return "ConsultationDraft(redacted)";
    }
}
