package io.github.rogeriocisi.airlab.anchor.internal;

import io.github.rogeriocisi.airlab.anchor.AnchorCheck;
import io.github.rogeriocisi.airlab.anchor.AnchorOutcome;
import io.github.rogeriocisi.airlab.anchor.AnchorService;
import io.github.rogeriocisi.airlab.anchor.AnchorStatus;
import io.github.rogeriocisi.airlab.anchor.AnchorView;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditHead;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bouncycastle.tsp.TimeStampRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The anchoring policy (SPEC E8, v0.15).
 *
 * <p>A stamp is due when the head of the trail is not stamped yet and either a milestone happened
 * after the last granted stamp, or anything happened and the last granted stamp is older than the
 * interval. The stamp's own event does not count as a change, or the trail would stamp itself
 * forever. After a failure nothing is sent until {@code retry-after} has passed.
 *
 * <p>The network call happens outside any database transaction; only the outcome is written, in a
 * short one, together with the trail event of a granted stamp.
 */
@Service
class DefaultAnchorService implements AnchorService {

    /** Events after which the head is stamped at the next check, without waiting for the interval. */
    static final Set<AuditEventType> MILESTONES = EnumSet.of(
            AuditEventType.PREREGISTRATION_FROZEN,
            AuditEventType.PREREGISTRATION_AMENDED,
            AuditEventType.ALLOCATION_SEED_COMMITTED,
            AuditEventType.ALLOCATION_DRAWN,
            AuditEventType.SEED_REVEALED,
            AuditEventType.UNBLINDING_PERFORMED,
            AuditEventType.REPORT_GENERATED);

    private final AnchorRepository anchors;
    private final AuditTrail trail;
    private final TsaClient client;
    private final TrustAnchors trust;
    private final TimestampProperties properties;
    private final TransactionTemplate transactions;

    DefaultAnchorService(AnchorRepository anchors, AuditTrail trail, TsaClient client, TrustAnchors trust,
                         TimestampProperties properties, PlatformTransactionManager transactionManager) {
        this.anchors = anchors;
        this.trail = trail;
        this.client = client;
        this.trust = trust;
        this.properties = properties;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public AnchorOutcome anchorIfDue(Instant now) {
        Optional<AuditHead> maybeHead = trail.head();
        if (maybeHead.isEmpty()) {
            return AnchorOutcome.notDue();
        }
        AuditHead head = maybeHead.get();
        Optional<AnchorEntity> lastGranted = anchors.findFirstByStatusOrderByIdDesc(AnchorEntity.GRANTED);
        if (!due(head, lastGranted, now)) {
            return AnchorOutcome.notDue();
        }
        Optional<AnchorEntity> lastAttempt = anchors.findFirstByOrderByIdDesc();
        if (lastAttempt.isPresent() && !lastAttempt.get().isGranted()
                && now.isBefore(lastAttempt.get().requestedAt().plus(properties.retryAfter()))) {
            return AnchorOutcome.notDue();
        }

        TimeStampRequest request = TimestampProtocol.request(Hashes.fromHex(head.hash()));
        try {
            byte[] response = client.exchange(request.getEncoded());
            TimestampProtocol.TokenInfo info = TimestampProtocol.checkResponse(response, request, trust);
            return AnchorOutcome.granted(transactions.execute(status -> store(head, now, response, info)));
        } catch (AnchorRejected e) {
            transactions.executeWithoutResult(status -> anchors.save(AnchorEntity.failed(
                    Codes.of("ANC"), head.position(), head.hash(), properties.url(), now, e.reason())));
            return AnchorOutcome.failed(e.reason());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("pedido de carimbo nao pode ser codificado", e);
        }
    }

    private boolean due(AuditHead head, Optional<AnchorEntity> lastGranted, Instant now) {
        long since = lastGranted.map(AnchorEntity::anchoredSeq).orElse(0L);
        if (lastGranted.isPresent() && since == head.position()) {
            return false;
        }
        Set<AuditEventType> after = EnumSet.noneOf(AuditEventType.class);
        after.addAll(trail.typesAfter(since));
        after.remove(AuditEventType.TIMESTAMP_ANCHORED);
        if (after.isEmpty()) {
            return false;
        }
        boolean milestone = after.stream().anyMatch(MILESTONES::contains);
        boolean intervalElapsed = lastGranted.isEmpty()
                || !now.isBefore(lastGranted.get().requestedAt().plus(properties.interval()));
        return milestone || intervalElapsed;
    }

    private AnchorView store(AuditHead head, Instant now, byte[] response, TimestampProtocol.TokenInfo info) {
        String responseSha256 = Hashes.toHex(Hashes.sha256(response));
        AnchorEntity saved = anchors.save(AnchorEntity.granted(Codes.of("ANC"), head.position(), head.hash(),
                properties.url(), now, info.attestedAt(), info.serialHex(), response, responseSha256));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("anchor", saved.code());
        payload.put("anchoredPosition", head.position());
        payload.put("anchoredHash", head.hash());
        payload.put("authority", properties.authority());
        payload.put("attestedAt", info.attestedAt().toString());
        payload.put("serial", info.serialHex());
        payload.put("responseSha256", responseSha256);
        trail.append(AuditEventType.TIMESTAMP_ANCHORED, ActorRole.SYSTEM, null, "anchor:" + saved.code(), payload);
        return view(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnchorView> granted() {
        return anchors.findByStatusOrderByIdDesc(AnchorEntity.GRANTED).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> response(String code) {
        return anchors.findByCodeAndStatus(code, AnchorEntity.GRANTED).map(AnchorEntity::responseDer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnchorCheck> verifyAll() {
        List<AnchorCheck> checks = new ArrayList<>();
        List<AnchorEntity> granted = new ArrayList<>(anchors.findByStatusOrderByIdDesc(AnchorEntity.GRANTED));
        java.util.Collections.reverse(granted);
        for (AnchorEntity anchor : granted) {
            boolean matchesTrail = trail.hashAt(anchor.anchoredSeq()).map(anchor.anchoredHash()::equals).orElse(false);
            boolean tokenValid;
            String detail;
            try {
                TimestampProtocol.TokenInfo info = TimestampProtocol.checkStored(
                        anchor.responseDer(), Hashes.fromHex(anchor.anchoredHash()), trust);
                tokenValid = info.attestedAt().equals(anchor.attestedAt());
                detail = tokenValid
                        ? "token confere, atestado em " + info.attestedAt()
                        : "token confere, mas a hora atestada nao e a registrada";
            } catch (AnchorRejected e) {
                tokenValid = false;
                detail = "TOKEN NAO CONFERE: " + e.getMessage();
            }
            if (!matchesTrail) {
                detail = detail + "; HASH NAO E O DO EVENTO " + anchor.anchoredSeq() + " DA TRILHA";
            }
            checks.add(new AnchorCheck(anchor.code(), anchor.anchoredSeq(), tokenValid, matchesTrail, detail));
        }
        return checks;
    }

    @Override
    @Transactional(readOnly = true)
    public AnchorStatus status() {
        Optional<AnchorEntity> lastGranted = anchors.findFirstByStatusOrderByIdDesc(AnchorEntity.GRANTED);
        long failures = anchors.countByStatusAndIdGreaterThan(AnchorEntity.FAILED,
                lastGranted.map(AnchorEntity::id).orElse(0L));
        Instant lastFailure = anchors.findFirstByStatusOrderByIdDesc(AnchorEntity.FAILED)
                .map(AnchorEntity::requestedAt).orElse(null);
        return new AnchorStatus(properties.enabled(), properties.authority(), properties.url(),
                trust.tsaSha256(), trust.caSha256(), anchors.countByStatus(AnchorEntity.GRANTED),
                lastGranted.map(AnchorEntity::attestedAt).orElse(null), failures, lastFailure);
    }

    private AnchorView view(AnchorEntity anchor) {
        return new AnchorView(anchor.code(), anchor.anchoredSeq(), anchor.anchoredHash(), anchor.attestedAt(),
                properties.authority(), anchor.serialHex(), anchor.responseSha256());
    }
}
