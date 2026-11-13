package io.github.rogeriocisi.airlab.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The closed questionnaire of the automated AIR protocol, P4 (SPEC E13). The sitter and the
 * medium answer the same fields; the system compares the answers field by field.
 *
 * <p>Versioned and immutable: an answer is meaningful only together with the version of the
 * questionnaire it answers, and that version is part of every commitment.
 *
 * <p>Closed fields only. Comparing free text would take a judgement, and P10 forbids handing
 * that judgement to a model; equality between options needs none.
 *
 * @param version version tag, recorded with every request and frozen in the preregistration
 * @param fields  fields, in the order they are shown
 */
public record Questionnaire(String version, List<Questionnaire.Field> fields) {

    /** The sitter's "I don't know". Never matches anything. */
    public static final String UNKNOWN = "nao-sei";

    /** The medium's "nothing came". Never matches anything. */
    public static final String NOT_RECEIVED = "nao-recebi";

    public Questionnaire {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("a questionnaire needs a version");
        }
        fields = List.copyOf(fields);
        Set<String> keys = new HashSet<>();
        for (Field field : fields) {
            if (!keys.add(field.key())) {
                throw new IllegalArgumentException("duplicate field " + field.key());
            }
        }
        if (fields.stream().noneMatch(field -> field.use() == Use.SCORED)) {
            throw new IllegalArgumentException("a questionnaire without scored fields scores nothing");
        }
    }

    public Optional<Field> field(String key) {
        return fields.stream().filter(field -> field.key().equals(key)).findFirst();
    }

    public List<Field> scoredFields() {
        return fields.stream().filter(field -> field.use() == Use.SCORED).toList();
    }

    /**
     * Checks a sitter's answers: every field answered, every option known, "não sei" only where
     * it is allowed, and nothing extra.
     *
     * @throws IllegalArgumentException with a message in Portuguese, for the person filling in
     */
    public void checkSitterAnswers(Map<String, String> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("Responda ao questionário.");
        }
        for (String key : answers.keySet()) {
            if (field(key).isEmpty()) {
                throw new IllegalArgumentException("Campo desconhecido no questionário.");
            }
        }
        for (Field field : fields) {
            String answer = answers.get(field.key());
            if (answer == null || answer.isBlank()) {
                throw new IllegalArgumentException("Responda ao campo \"" + field.label() + "\".");
            }
            if (UNKNOWN.equals(answer)) {
                if (!field.unknownAllowed()) {
                    throw new IllegalArgumentException("O campo \"" + field.label()
                            + "\" é usado para formar os pares e precisa de uma resposta.");
                }
                continue;
            }
            if (field.option(answer).isEmpty()) {
                throw new IllegalArgumentException("Opção desconhecida no campo \"" + field.label() + "\".");
            }
        }
    }

    /**
     * Checks a medium's answers (SPEC E13, phase C): every scored field answered with an option
     * or "não recebi", and nothing else. The pairing fields are not asked of the medium — they do
     * not count, and within a pair they are equal or given away by the first name — and "não sei"
     * is the sitter's answer, not the medium's.
     *
     * @throws IllegalArgumentException with a message in Portuguese, for the person filling in
     */
    public void checkMediumAnswers(Map<String, String> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("Responda ao questionário.");
        }
        for (String key : answers.keySet()) {
            if (field(key).filter(Field::scored).isEmpty()) {
                throw new IllegalArgumentException("Campo desconhecido no questionário.");
            }
        }
        for (Field field : scoredFields()) {
            String answer = answers.get(field.key());
            if (answer == null || answer.isBlank()) {
                throw new IllegalArgumentException("Responda ao campo \"" + field.label()
                        + "\": escolha uma opção ou \"Não recebi\".");
            }
            if (!NOT_RECEIVED.equals(answer) && field.option(answer).isEmpty()) {
                throw new IllegalArgumentException("Opção desconhecida no campo \"" + field.label() + "\".");
            }
        }
    }

    /** Label of an answer, for showing it back; "Não sei" and "Não recebi" included. */
    public String labelOf(String fieldKey, String answer) {
        if (UNKNOWN.equals(answer)) {
            return "Não sei";
        }
        if (NOT_RECEIVED.equals(answer)) {
            return "Não recebi";
        }
        return field(fieldKey).flatMap(field -> field.option(answer)).map(Option::label).orElse(answer);
    }

    /** What a field is for. */
    public enum Use {
        /** Counts in the comparison between reading and target. */
        SCORED,
        /**
         * Used to form the pairs and shown in the feedback, but out of the score: it is equal
         * within a pair by construction, or the first name gives it away.
         */
        PAIRING
    }

    /**
     * One field.
     *
     * @param key             stable key, part of the canonical answers
     * @param label           question, in Portuguese
     * @param use             scored or pairing
     * @param outOfScoreReason why a pairing field does not count, shown on the page; null for scored
     * @param options         closed options
     */
    public record Field(String key, String label, Use use, String outOfScoreReason, List<Option> options) {

        public Field {
            options = List.copyOf(options);
            if (options.size() < 2) {
                throw new IllegalArgumentException("field " + key + " needs at least two options");
            }
            Set<String> keys = new HashSet<>();
            for (Option option : options) {
                if (!keys.add(option.key()) || UNKNOWN.equals(option.key())
                        || NOT_RECEIVED.equals(option.key())) {
                    throw new IllegalArgumentException("bad option " + option.key() + " in " + key);
                }
            }
            if ((use == Use.PAIRING) == (outOfScoreReason == null || outOfScoreReason.isBlank())) {
                throw new IllegalArgumentException(
                        "field " + key + ": a pairing field states why it is out of the score, a scored one does not");
            }
        }

        /** The sitter may answer "não sei" only where the field is not needed for pairing. */
        public boolean unknownAllowed() {
            return use == Use.SCORED;
        }

        public boolean scored() {
            return use == Use.SCORED;
        }

        public Optional<Option> option(String key) {
            return options.stream().filter(option -> option.key().equals(key)).findFirst();
        }
    }

    /** One closed option. */
    public record Option(String key, String label) {
    }
}
