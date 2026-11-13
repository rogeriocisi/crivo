package io.github.rogeriocisi.airlab.structured;

import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The commitment of a medium's P4 reading (SPEC E13, phase C), public so that anyone can
 * recompute it once the reading and its salt are revealed.
 *
 * <pre>
 * commitment = SHA-256( canonical || "|" || saltHex )
 * canonical  = CanonicalJson{ "answers": {field: option, ...}, "protocol": "P4",
 *                             "questionnaire": version, "reading": code, "request": code }
 * </pre>
 *
 * <p>Salted for the same reason as the sitter's request: closed answers have little entropy,
 * and the trail is public. The request code is inside, so a reading cannot later be moved to
 * the other name of its pair.
 */
public final class ReadingCommitment {

    private ReadingCommitment() {
    }

    public static String canonical(String questionnaireVersion, String readingCode, String requestCode,
                                   Map<String, String> answers) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("protocol", "P4");
        document.put("questionnaire", questionnaireVersion);
        document.put("reading", readingCode);
        document.put("request", requestCode);
        document.put("answers", new TreeMap<>(answers));
        return CanonicalJson.write(document);
    }

    public static String of(String questionnaireVersion, String readingCode, String requestCode,
                            Map<String, String> answers, String saltHex) {
        if (saltHex == null || !saltHex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("the salt must be 32 bytes in lower-case hex");
        }
        return Hashes.sha256Hex(canonical(questionnaireVersion, readingCode, requestCode, answers) + "|" + saltHex);
    }
}
