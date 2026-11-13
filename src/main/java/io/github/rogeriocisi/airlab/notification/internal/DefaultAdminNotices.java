package io.github.rogeriocisi.airlab.notification.internal;

import io.github.rogeriocisi.airlab.notification.AdminNotice;
import io.github.rogeriocisi.airlab.notification.AdminNotices;
import io.github.rogeriocisi.airlab.notification.Mailer;
import io.github.rogeriocisi.airlab.notification.NoticeKind;
import io.github.rogeriocisi.airlab.participant.AccountConfirmed;
import io.github.rogeriocisi.airlab.participant.ConsultationLocked;
import io.github.rogeriocisi.airlab.structured.PairReadingsReceived;
import io.github.rogeriocisi.airlab.structured.ReadingRequestExpired;
import io.github.rogeriocisi.airlab.structured.ReadingRequestSent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notices to the administrator. Stored in {@code research.admin_notice} (V17) and, when an
 * address is configured, also sent by e-mail — with the same three fields and nothing more.
 */
@Service
class DefaultAdminNotices implements AdminNotices {

    private final AdminNoticeRepository notices;
    private final Mailer mailer;
    private final Clock clock;
    private final String adminEmail;

    DefaultAdminNotices(AdminNoticeRepository notices, Mailer mailer, Clock clock,
                        Environment environment) {
        this.notices = notices;
        this.mailer = mailer;
        this.clock = clock;
        this.adminEmail = environment.getProperty("airlab.admin-email", "").strip();
    }

    @Override
    @Transactional
    public void record(NoticeKind kind, String subjectCode) {
        Instant now = clock.instant();
        notices.save(new AdminNoticeEntity(kind, subjectCode, now));
        if (!adminEmail.isEmpty() && mailer.enabled()) {
            mailer.send(adminEmail,
                    "Crivo: " + kind.label() + " (" + subjectCode + ")",
                    """
                    Aviso da plataforma Crivo.

                    Etapa concluída: %s
                    Código: %s
                    Quando: %s (UTC)

                    Este aviso traz só o tipo, o código pseudônimo e a hora, de propósito: e-mail não
                    é um canal cego. Os detalhes ficam no painel do administrador.
                    """.formatted(kind.label(), subjectCode, now));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotice> recent(int limit) {
        return notices.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(entity -> new AdminNotice(entity.kind, entity.subjectCode, entity.createdAt))
                .toList();
    }

    @EventListener
    void on(AccountConfirmed event) {
        record(NoticeKind.ACCOUNT_CONFIRMED, event.code());
    }

    @EventListener
    void on(ConsultationLocked event) {
        record(NoticeKind.REQUEST_LOCKED, event.code());
    }

    // Phase C: the notice carries the pair code, never the medium's (P4 matrix).

    @EventListener
    void on(ReadingRequestSent event) {
        record(NoticeKind.PAIR_SENT_TO_MEDIUM, event.pairCode());
    }

    @EventListener
    void on(PairReadingsReceived event) {
        record(NoticeKind.READING_RECEIVED, event.pairCode());
    }

    @EventListener
    void on(ReadingRequestExpired event) {
        record(NoticeKind.READING_OVERDUE, event.pairCode());
    }
}

@Entity
@Table(name = "admin_notice", schema = "research")
class AdminNoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 48)
    NoticeKind kind;

    @Column(name = "subject_code", nullable = false, length = 32)
    String subjectCode;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected AdminNoticeEntity() {
    }

    AdminNoticeEntity(NoticeKind kind, String subjectCode, Instant createdAt) {
        this.kind = kind;
        this.subjectCode = subjectCode;
        this.createdAt = createdAt;
    }
}

interface AdminNoticeRepository extends JpaRepository<AdminNoticeEntity, Long> {

    List<AdminNoticeEntity> findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.Pageable page);
}
