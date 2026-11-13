package io.github.rogeriocisi.airlab.structured;

import java.util.List;

/**
 * Domain event: a P4 study was unblinded and its report written (SPEC E13, phase D). In-process
 * only: the notification module records the administrator's notices and tells the sitters and
 * mediums of the study, by e-mail and without any score, that their feedback is ready.
 */
public record StructuredStudyReported(String studyCode, List<String> sitterCodes, List<String> mediumCodes) {

    public StructuredStudyReported {
        sitterCodes = List.copyOf(sitterCodes);
        mediumCodes = List.copyOf(mediumCodes);
    }
}
