package io.github.rogeriocisi.airlab.structured;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * A medium's reading while it is being filled in and reviewed. Lives only in the HTTP session;
 * the database receives the reading once, already locked (SPEC E13, phase C).
 */
public record MediumReadingDraft(String assignmentCode, int position, Map<String, String> answers)
        implements Serializable {

    public MediumReadingDraft {
        answers = new TreeMap<>(answers == null ? Map.of() : answers);
    }

    @Override
    public Map<String, String> answers() {
        return Map.copyOf(answers);
    }

    @Override
    public String toString() {
        return "MediumReadingDraft[assignment=" + assignmentCode + ", position=" + position + ", redacted]";
    }
}
