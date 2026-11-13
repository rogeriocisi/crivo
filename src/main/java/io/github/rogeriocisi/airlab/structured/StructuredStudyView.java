package io.github.rogeriocisi.airlab.structured;

import io.github.rogeriocisi.airlab.study.StudyStatus;
import java.time.Instant;

/**
 * The P4 study as the administrator sees it: what was frozen, with the hashes to check it. The
 * preregistration document is public by design (P2, P7), so it can be shown whole.
 */
public record StructuredStudyView(long studyId,
                                  String studyCode,
                                  String title,
                                  StudyStatus status,
                                  String questionnaireVersion,
                                  int targetPairs,
                                  int deadlineDays,
                                  String documentSha256,
                                  String seedCommitment,
                                  Instant frozenAt,
                                  String document) {
}
