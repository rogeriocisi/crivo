package io.github.rogeriocisi.airlab.allocation.internal;

import io.github.rogeriocisi.airlab.allocation.AllocationDrawn;
import io.github.rogeriocisi.airlab.allocation.AllocationEngine;
import io.github.rogeriocisi.airlab.allocation.AllocationPlan;
import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.allocation.AllocationSnapshot;
import io.github.rogeriocisi.airlab.allocation.DrawInput;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultAllocationService implements AllocationService {

    private final AllocationSetRepository sets;
    private final AuditTrail auditTrail;
    private final FieldCipher cipher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    DefaultAllocationService(AllocationSetRepository sets,
                             AuditTrail auditTrail,
                             FieldCipher cipher,
                             ApplicationEventPublisher events,
                             Clock clock) {
        this.sets = sets;
        this.auditTrail = auditTrail;
        this.cipher = cipher;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String draw(long studyId, DrawInput input) {
        if (sets.findByStudyId(studyId).isPresent()) {
            throw new IllegalStateException("study " + studyId + " was already drawn; a redraw would void the commitment");
        }

        String seedHex = AllocationEngine.newSeedHex();
        AllocationPlan plan = AllocationEngine.plan(seedHex, input);
        String commitment = AllocationEngine.commitment(seedHex, plan);

        AllocationSet set = new AllocationSet(
                studyId,
                commitment,
                input.canonicalForm(),
                plan.canonicalForm(),
                cipher.encrypt(seedHex),
                clock.instant());
        sets.save(set);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commitment", commitment);
        payload.put("drawInputSha256", Hashes.sha256Hex(input.canonicalForm()));
        payload.put("allocationsSha256", Hashes.sha256Hex(plan.canonicalForm()));
        payload.put("pairs", input.pairs().size());
        payload.put("sessions", plan.sessions().size());
        // The plan itself is not in the trail: the trail is public (P7) and the plan is
        // blinding sensitive until the unblinding (P1).
        auditTrail.append(AuditEventType.ALLOCATION_DRAWN, ActorRole.SYSTEM, null,
                "study:" + studyId, payload);

        events.publishEvent(new AllocationDrawn(studyId, commitment, plan));
        return commitment;
    }

    @Override
    @Transactional
    public String commitSeed(long studyId, String studyCode) {
        if (sets.findByStudyId(studyId).isPresent()) {
            throw new IllegalStateException("study " + studyId + " already has a committed seed");
        }
        String seedHex = AllocationEngine.newSeedHex();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("mode", "P4_INCREMENTAL");
        input.put("studyCode", studyCode);
        String canonicalInput = CanonicalJson.write(input);
        String commitment = AllocationEngine.seedCommitment(seedHex, canonicalInput);

        // No plan yet: the P4 draws happen during collection and are recorded where they happen,
        // with their inputs. This row holds the seed and its commitment.
        sets.save(new AllocationSet(studyId, commitment, canonicalInput, "[]",
                cipher.encrypt(seedHex), clock.instant()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commitment", commitment);
        payload.put("mode", "P4_INCREMENTAL");
        payload.put("drawInputSha256", Hashes.sha256Hex(canonicalInput));
        auditTrail.append(AuditEventType.ALLOCATION_SEED_COMMITTED, ActorRole.SYSTEM, null,
                "study:" + studyId, payload);
        return commitment;
    }

    @Override
    @Transactional(readOnly = true)
    public String drawStructuredMedium(long studyId, String pairCode, int attempt, List<String> eligibleMediums) {
        return AllocationEngine.structuredMedium(sealedSeed(studyId), pairCode, attempt, eligibleMediums);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> structuredNameOrder(long studyId, String pairCode, int attempt, List<String> requestCodes) {
        return AllocationEngine.structuredNameOrder(sealedSeed(studyId), pairCode, attempt, requestCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> commitment(long studyId) {
        return sets.findByStudyId(studyId).map(AllocationSet::seedCommitment);
    }

    @Override
    @Transactional
    public String revealSeed(long studyId, String actorCode) {
        AllocationSet set = sets.findByStudyId(studyId)
                .orElseThrow(() -> new IllegalStateException("no draw for study " + studyId));
        if (set.seedHex() != null) {
            return set.seedHex();
        }
        String seedHex = cipher.decrypt(set.seedEscrow());
        set.reveal(seedHex, clock.instant());
        sets.save(set);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commitment", set.seedCommitment());
        payload.put("seedSha256", Hashes.sha256Hex(seedHex));
        auditTrail.append(AuditEventType.SEED_REVEALED, ActorRole.RESEARCHER, actorCode,
                "study:" + studyId, payload);
        return seedHex;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> itemPresentationOrder(long studyId, String redactedReadingCode,
                                               int itemCount) {
        return AllocationEngine.itemPresentationOrder(
                sealedSeed(studyId), redactedReadingCode, itemCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> balancedItemSelection(long studyId, String redactedReadingCode,
                                               int itemCount, int keep) {
        return AllocationEngine.balancedSubsample(
                sealedSeed(studyId), redactedReadingCode, itemCount, keep);
    }

    /**
     * Reads the escrowed seed for an internal derivation. Private on purpose: the seed leaves
     * this module only through {@link #revealSeed}, at the unblinding (P1, P5).
     */
    private String sealedSeed(long studyId) {
        AllocationSet set = sets.findByStudyId(studyId)
                .orElseThrow(() -> new IllegalStateException("no draw for study " + studyId));
        return cipher.decrypt(set.seedEscrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AllocationSnapshot> snapshot(long studyId) {
        return sets.findByStudyId(studyId).map(set -> new AllocationSnapshot(
                set.studyId(),
                set.seedCommitment(),
                set.drawInput(),
                set.allocations(),
                set.drawnAt(),
                Optional.ofNullable(set.seedHex()),
                Optional.ofNullable(set.seedRevealedAt())));
    }
}
