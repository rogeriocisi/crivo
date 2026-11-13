package io.github.rogeriocisi.airlab.participant;

import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The commitment of a P4 consultation request (SPEC E13, phase B), public so that anyone can
 * recompute it from the revealed request.
 *
 * <pre>
 * commitment = SHA-256( canonical || "|" || saltHex )
 * canonical  = CanonicalJson{ "answers": {field: option, ...}, "firstName": ...,
 *                             "protocol": "P4", "questionnaire": version }
 * </pre>
 *
 * <p>The salt is what makes the commitment safe to publish: closed answers have little
 * entropy, and the hash of a handful of options alone would fall to a dictionary search over
 * the public trail. With 32 random bytes in the hash, it does not.
 */
public final class ConsultationCommitment {

    private ConsultationCommitment() {
    }

    public static String canonical(String questionnaireVersion, String firstName, Map<String, String> answers) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("protocol", "P4");
        document.put("questionnaire", questionnaireVersion);
        document.put("firstName", firstName);
        document.put("answers", new TreeMap<>(answers));
        return CanonicalJson.write(document);
    }

    public static String of(String questionnaireVersion, String firstName, Map<String, String> answers,
                            String saltHex) {
        if (saltHex == null || !saltHex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("the salt must be 32 bytes in lower-case hex");
        }
        return Hashes.sha256Hex(canonical(questionnaireVersion, firstName, answers) + "|" + saltHex);
    }
}
