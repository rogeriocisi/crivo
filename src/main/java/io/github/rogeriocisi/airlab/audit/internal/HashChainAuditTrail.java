package io.github.rogeriocisi.airlab.audit.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.audit.ChainVerification;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.Hashes;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class HashChainAuditTrail implements AuditTrail {

    /** Arbitrary but fixed key for the advisory lock that serialises appends. */
    private static final long APPEND_LOCK = 8_314_190_001L;

    private final AuditEventRepository events;
    private final EntityManager entityManager;
    private final Clock clock;

    HashChainAuditTrail(AuditEventRepository events, EntityManager entityManager, Clock clock) {
        this.events = events;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String append(AuditEventType type,
                         ActorRole actorRole,
                         String actorCode,
                         String subjectRef,
                         Map<String, Object> payload) {
        // Two concurrent appends must not read the same head, or the chain would fork.
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", APPEND_LOCK)
                .getSingleResult();

        String prevHash = events.findFirstByOrderBySeqDesc().map(AuditEvent::hash).orElse(Hashes.ZERO);
        AuditEvent event = new AuditEvent(
                // Truncated to microseconds, the precision PostgreSQL stores: the hash has to
                // be computed over the very value that is written, or re-reading the row would
                // recompute a different hash and the chain would look broken.
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                type,
                actorRole,
                actorCode,
                subjectRef,
                CanonicalJson.write(payload == null ? Map.of() : payload),
                prevHash);
        return events.save(event).hash();
    }

    @Override
    @Transactional(readOnly = true)
    public ChainVerification verifyChain() {
        long checked = 0;
        String expectedPrev = Hashes.ZERO;
        try (Stream<AuditEvent> stream = events.streamAllOrdered()) {
            for (AuditEvent event : (Iterable<AuditEvent>) stream::iterator) {
                checked++;
                if (!expectedPrev.equals(event.prevHash())) {
                    return ChainVerification.broken(checked, event.seq(),
                            "Evento " + event.seq() + " aponta para um antecessor que nao e o anterior da cadeia.");
                }
                String recomputed = event.computeHash();
                if (!recomputed.equals(event.hash())) {
                    return ChainVerification.broken(checked, event.seq(),
                            "Evento " + event.seq() + " foi alterado: o hash recalculado nao confere com o gravado.");
                }
                expectedPrev = event.hash();
            }
        }
        return ChainVerification.intact(checked);
    }

    @Override
    @Transactional(readOnly = true)
    public String headHash() {
        return events.findFirstByOrderBySeqDesc().map(AuditEvent::hash).orElse(Hashes.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public long size() {
        return events.count();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<io.github.rogeriocisi.airlab.audit.AuditHead> head() {
        return events.findFirstByOrderBySeqDesc().map(event ->
                new io.github.rogeriocisi.airlab.audit.AuditHead(event.seq(), event.hash(), event.occurredAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<AuditEventType> typesAfter(long position) {
        java.util.List<AuditEventType> types = events.distinctTypesAfter(position);
        return types.isEmpty() ? java.util.Set.of() : java.util.EnumSet.copyOf(types);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> hashAt(long position) {
        return events.findById(position).map(AuditEvent::hash);
    }
}
