package io.github.rogeriocisi.airlab.study.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.study.PreregistrationPlan;
import io.github.rogeriocisi.airlab.study.PreregistrationSnapshot;
import io.github.rogeriocisi.airlab.study.ProtocolDeviation;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudyStatus;
import io.github.rogeriocisi.airlab.study.StudySummary;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
class DefaultStudyService implements StudyService {

    /** v1 implements a single protocol: the blind paired reading (SPEC section 1). */
    private static final String PROTOCOL_V1 = "PAIRED_READING_V1";

    private final StudyRepository studies;
    private final PreregistrationVersionRepository versions;
    private final ProtocolDeviationRepository deviations;
    private final AuditTrail auditTrail;
    private final Clock clock;

    DefaultStudyService(StudyRepository studies,
                        PreregistrationVersionRepository versions,
                        ProtocolDeviationRepository deviations,
                        AuditTrail auditTrail,
                        Clock clock) {
        this.studies = studies;
        this.versions = versions;
        this.deviations = deviations;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StudySummary create(String title, PreregistrationPlan plan, String actorCode) {
        return create(title, PROTOCOL_V1, plan, null, actorCode);
    }

    @Override
    @Transactional
    public StudySummary create(String title, String protocol, PreregistrationPlan plan, String protocolAnnex,
                               String actorCode) {
        StudyEntity study = studies.save(
                new StudyEntity(Codes.of("STD"), title, protocol, clock.instant()));

        String document = PreregistrationDocument.render(
                study.code(), study.title(), study.protocol(), 1, plan, null, null, protocolAnnex);
        PreregistrationVersionEntity version = versions.save(new PreregistrationVersionEntity(
                study.id(), 1, document, Hashes.sha256Hex(document), null, clock.instant()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("study", study.code());
        payload.put("protocol", study.protocol());
        auditTrail.append(AuditEventType.STUDY_CREATED, ActorRole.RESEARCHER, actorCode,
                "study:" + study.id(), payload);

        Map<String, Object> draftPayload = new LinkedHashMap<>();
        draftPayload.put("version", version.version());
        draftPayload.put("documentSha256", version.documentSha256());
        auditTrail.append(AuditEventType.PREREGISTRATION_DRAFTED, ActorRole.RESEARCHER, actorCode,
                "study:" + study.id(), draftPayload);

        return summary(study);
    }

    @Override
    @Transactional
    public PreregistrationSnapshot freeze(long studyId, String allocationCommitment, String actorCode) {
        StudyEntity study = study(studyId);
        PreregistrationVersionEntity version = versions.findFirstByStudyIdOrderByVersionDesc(studyId)
                .orElseThrow(() -> new IllegalStateException("study " + studyId + " has no preregistration"));
        if (version.frozenAt() != null) {
            throw new IllegalStateException("preregistration of study " + studyId + " is already frozen");
        }
        if (allocationCommitment == null || allocationCommitment.isBlank()) {
            // P2 + P5: the commitment is what binds the plan to the draw. Freezing without
            // it would leave the allocations outside the preregistration.
            throw new IllegalArgumentException("freezing requires the allocation commitment");
        }

        String document = reRender(study, version, allocationCommitment);
        version.freeze(document, Hashes.sha256Hex(document), clock.instant());
        versions.save(version);
        study.status(StudyStatus.PREREGISTERED);
        studies.save(study);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", version.version());
        payload.put("documentSha256", version.documentSha256());
        payload.put("allocationCommitment", allocationCommitment);
        auditTrail.append(AuditEventType.PREREGISTRATION_FROZEN, ActorRole.RESEARCHER, actorCode,
                "study:" + studyId, payload);

        return snapshot(version);
    }

    @Override
    @Transactional
    public PreregistrationSnapshot amend(long studyId, PreregistrationPlan plan, String reason,
                                         String actorCode) {
        StudyEntity study = study(studyId);
        if (!PROTOCOL_V1.equals(study.protocol())) {
            // An amendment re-renders the document from the plan; for another protocol it would
            // drop the annex, and with it the questionnaire the study froze.
            throw new IllegalStateException("amendments are implemented for " + PROTOCOL_V1 + " only");
        }
        int nextVersion = versions.findFirstByStudyIdOrderByVersionDesc(studyId)
                .map(PreregistrationVersionEntity::version).orElse(0) + 1;

        String document = PreregistrationDocument.render(
                study.code(), study.title(), study.protocol(), nextVersion, plan, null, reason, null);
        PreregistrationVersionEntity version = versions.save(new PreregistrationVersionEntity(
                study.id(), nextVersion, document, Hashes.sha256Hex(document), reason, clock.instant()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", nextVersion);
        payload.put("documentSha256", version.documentSha256());
        payload.put("reasonSha256", Hashes.sha256Hex(reason));
        auditTrail.append(AuditEventType.PREREGISTRATION_AMENDED, ActorRole.RESEARCHER, actorCode,
                "study:" + studyId, payload);

        return snapshot(version);
    }

    @Override
    @Transactional
    public void advanceStatus(long studyId, StudyStatus target, String actorCode) {
        advanceStatus(studyId, target, ActorRole.RESEARCHER, actorCode);
    }

    @Override
    @Transactional
    public void advanceStatus(long studyId, StudyStatus target, ActorRole role, String actorCode) {
        StudyEntity study = study(studyId);
        study.status(target);
        studies.save(study);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", target.name());
        auditTrail.append(AuditEventType.STUDY_STATUS_ADVANCED, role, actorCode,
                "study:" + studyId, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudySummary> byId(long studyId) {
        return studies.findById(studyId).map(this::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudySummary> byCode(String code) {
        return studies.findByCode(code).map(this::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudySummary> all() {
        List<StudySummary> out = new ArrayList<>();
        studies.findAllByOrderByCreatedAtDesc().forEach(s -> out.add(summary(s)));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PreregistrationSnapshot> currentPreregistration(long studyId) {
        return versions.findFirstByStudyIdOrderByVersionDesc(studyId).map(this::snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreregistrationSnapshot> preregistrationHistory(long studyId) {
        List<PreregistrationSnapshot> out = new ArrayList<>();
        versions.findByStudyIdOrderByVersionAsc(studyId).forEach(v -> out.add(snapshot(v)));
        return out;
    }

    @Override
    @Transactional
    public ProtocolDeviation recordDeviation(long studyId, ProtocolDeviation.Kind kind,
                                             String description, Instant occurredAt,
                                             String actorCode) {
        int version = versions.findFirstByStudyIdOrderByVersionDesc(studyId)
                .map(PreregistrationVersionEntity::version)
                .orElseThrow(() -> new IllegalStateException(
                        "study " + studyId + " has no preregistration to deviate from"));

        ProtocolDeviationEntity entity = deviations.save(new ProtocolDeviationEntity(
                studyId, version, kind, description, occurredAt, clock.instant(), actorCode));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", kind.name());
        payload.put("preregistrationVersion", version);
        payload.put("occurredAt", occurredAt.toString());
        // The text itself is hashed rather than copied: the trail is public (P7) and a
        // deviation may describe a participant's withdrawal.
        payload.put("descriptionSha256", Hashes.sha256Hex(description));
        auditTrail.append(AuditEventType.PROTOCOL_DEVIATION_RECORDED, ActorRole.RESEARCHER,
                actorCode, "study:" + studyId, payload);

        return entity.toRecord();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProtocolDeviation> deviations(long studyId) {
        List<ProtocolDeviation> out = new ArrayList<>();
        deviations.findByStudyIdOrderByOccurredAtAsc(studyId)
                .forEach(entity -> out.add(entity.toRecord()));
        return out;
    }

    /**
     * Re-renders the stored document with the commitment inserted. The plan itself is read
     * back from the document's canonical JSON block, so freezing cannot silently change the
     * plan: only the commitment line may differ.
     */
    private String reRender(StudyEntity study, PreregistrationVersionEntity version, String commitment) {
        String document = version.document();
        String marker = "- Compromisso do sorteio (SHA-256): (nao congelado)";
        if (!document.contains(marker)) {
            throw new IllegalStateException("unexpected preregistration document shape for study " + study.id());
        }
        return document.replace(marker, "- Compromisso do sorteio (SHA-256): " + commitment);
    }

    private StudyEntity study(long studyId) {
        return studies.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("unknown study " + studyId));
    }

    private StudySummary summary(StudyEntity study) {
        return new StudySummary(study.id(), study.code(), study.title(), study.protocol(),
                study.status(), study.createdAt());
    }

    private PreregistrationSnapshot snapshot(PreregistrationVersionEntity version) {
        return new PreregistrationSnapshot(
                version.studyId(),
                version.version(),
                version.document(),
                version.documentSha256(),
                Optional.ofNullable(version.frozenAt()),
                Optional.ofNullable(version.timestampProofRef()),
                Optional.ofNullable(version.amendmentReason()),
                version.createdAt());
    }
}
