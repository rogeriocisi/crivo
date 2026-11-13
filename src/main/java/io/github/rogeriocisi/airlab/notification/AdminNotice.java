package io.github.rogeriocisi.airlab.notification;

import java.time.Instant;

/**
 * One notice, as the administrator sees it: kind, pseudonymous code and time. There is no
 * field for anything else, so nothing else can be shown (architectural rule 3).
 */
public record AdminNotice(NoticeKind kind, String subjectCode, Instant at) {
}
