package io.github.rogeriocisi.airlab.notification;

import java.util.List;

/**
 * Notices to the administrator: one per completed step, shown in the panel and, when
 * {@code AIRLAB_ADMIN_EMAIL} is set and e-mail works, also sent by e-mail.
 */
public interface AdminNotices {

    void record(NoticeKind kind, String subjectCode);

    /** Most recent notices, newest first. */
    List<AdminNotice> recent(int limit);
}
