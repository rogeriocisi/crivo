package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.participant.ConsentDeclaration;
import io.github.rogeriocisi.airlab.participant.PairView;
import io.github.rogeriocisi.airlab.participant.ParticipantRole;
import io.github.rogeriocisi.airlab.participant.ParticipantService;
import io.github.rogeriocisi.airlab.participant.PersonalData;
import io.github.rogeriocisi.airlab.participant.TargetProfile;
import io.github.rogeriocisi.airlab.participant.TargetRegistration;
import io.github.rogeriocisi.airlab.support.BlindIndex;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
class DefaultParticipantService implements ParticipantService {

    private final ParticipantEntityRepository participants;
    private final TargetDetailRepository targetDetails;
    private final ConsentRepository consents;
    private final ParticipantRoleRepository roles;
    private final TargetRepository targets;
    private final PairRepository pairs;
    private final FieldCipher cipher;
    private final BlindIndex blindIndex;
    private final AuditTrail auditTrail;
    private final Clock clock;

    DefaultParticipantService(ParticipantEntityRepository participants,
                              TargetDetailRepository targetDetails,
                              ConsentRepository consents,
                              ParticipantRoleRepository roles,
                              TargetRepository targets,
                              PairRepository pairs,
                              FieldCipher cipher,
                              BlindIndex blindIndex,
                              AuditTrail auditTrail,
                              Clock clock) {
        this.participants = participants;
        this.targetDetails = targetDetails;
        this.consents = consents;
        this.roles = roles;
        this.targets = targets;
        this.pairs = pairs;
        this.cipher = cipher;
        this.blindIndex = blindIndex;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String enrol(long studyId, ParticipantRole role, PersonalData personalData,
                        ConsentDeclaration consent, String actorCode) {
        String fingerprint = fingerprintOf(personalData);
        refuseConflictingRole(studyId, role, fingerprint, actorCode);

        String code = Codes.of(prefixFor(role));
        participants.save(new ParticipantEntity(code,
                cipher.encrypt(personalData.fullName()),
                cipher.encrypt(personalData.contact()),
                fingerprint,
                clock.instant()));
        roles.save(new ParticipantRoleEntity(studyId, code, role));
        consents.save(new ConsentEntity(code, consent.termVersion(),
                consent.sensitiveDataAccepted(), clock.instant()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", role.name());
        payload.put("participant", code);
        auditTrail.append(AuditEventType.PARTICIPANT_ENROLLED, ActorRole.RESEARCHER, actorCode,
                "study:" + studyId, payload);

        Map<String, Object> consentPayload = new LinkedHashMap<>();
        consentPayload.put("participant", code);
        consentPayload.put("termVersion", consent.termVersion());
        consentPayload.put("sensitiveDataAccepted", consent.sensitiveDataAccepted());
        auditTrail.append(AuditEventType.CONSENT_GRANTED, ActorRole.RESEARCHER, actorCode,
                "study:" + studyId, consentPayload);

        return code;
    }

    /**
     * Epic E10, lock 2: nobody holds two roles in the same study, and a second enrolment in the
     * same role is refused too.
     *
     * <p>With open enrolment this is the fraud that matters: the same person signing up as
     * medium and as consulente would hand the study a result that looks clean and means nothing.
     * The refusal is recorded in the trail, because a blocked attempt is information — a burst of
     * them says something is going on.
     *
     * <p>The check needs a contact to compare. An enrolment without one is allowed and flagged in
     * the trail, so that a study which skipped the check cannot later claim it was applied.
     */
    private void refuseConflictingRole(long studyId, ParticipantRole role, String fingerprint,
                                       String actorCode) {
        if (fingerprint == null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("role", role.name());
            payload.put("conflictCheck", "SKIPPED_NO_CONTACT");
            auditTrail.append(AuditEventType.PARTICIPANT_ENROLLED, ActorRole.RESEARCHER, actorCode,
                    "study:" + studyId, payload);
            return;
        }

        List<String> knownCodes = new ArrayList<>();
        participants.findByContactFingerprint(fingerprint)
                .forEach(existing -> knownCodes.add(existing.code()));
        if (knownCodes.isEmpty()) {
            return;
        }

        for (ParticipantRoleEntity held : roles.findByStudyIdAndCodeIn(studyId, knownCodes)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("attemptedRole", role.name());
            payload.put("heldRole", held.role().name());
            payload.put("heldBy", held.code());
            auditTrail.append(AuditEventType.ROLE_CONFLICT_REFUSED, ActorRole.SYSTEM, actorCode,
                    "study:" + studyId, payload);
            // No personal data in the message (P6): the codes are pseudonymous.
            throw new IllegalStateException(
                    "this person already holds the role " + held.role().name() + " in study "
                            + studyId + " (as " + held.code() + "); one role per person per study");
        }
    }

    private String fingerprintOf(PersonalData personalData) {
        String contact = personalData.contact();
        if (contact == null || contact.isBlank()) {
            return null;
        }
        return blindIndex.of("participant-contact", contact);
    }

    @Override
    @Transactional
    public String registerTarget(long studyId, String sitterCode, TargetRegistration registration,
                                 String actorCode) {
        String code = Codes.of("TGT");
        targetDetails.save(new TargetDetailEntity(code,
                cipher.encrypt(registration.firstName()),
                cipher.encrypt(registration.notes()),
                clock.instant()));
        targets.save(new TargetEntity(studyId, code, sitterCode, registration, clock.instant()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", code);
        payload.put("sitter", sitterCode);
        payload.put("gender", registration.gender().name());
        payload.put("ageBand", registration.ageBand().name());
        payload.put("deathPeriod", registration.deathPeriod().name());
        auditTrail.append(AuditEventType.TARGET_REGISTERED, ActorRole.SITTER, sitterCode,
                "study:" + studyId, payload);

        return code;
    }

    @Override
    @Transactional
    public String releaseFirstName(String targetCode, String experimenterCode, String sessionCode) {
        TargetDetailEntity detail = targetDetails.findByTargetCode(targetCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown target"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", targetCode);
        payload.put("session", sessionCode);
        // The name itself is not in the payload: the trail is public (P7).
        auditTrail.append(AuditEventType.FIRST_NAME_RELEASED, ActorRole.EXPERIMENTER, experimenterCode,
                "session:" + sessionCode, payload);

        return cipher.decrypt(detail.firstNameEnc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetProfile> targets(long studyId) {
        List<TargetProfile> out = new ArrayList<>();
        for (TargetEntity target : targets.findByStudyIdOrderByCodeAsc(studyId)) {
            out.add(new TargetProfile(target.code(), target.sitterCode(), target.gender(),
                    target.ageBand(), target.deathPeriod()));
        }
        return out;
    }

    @Override
    @Transactional
    public List<PairView> formPairs(long studyId, String actorCode) {
        if (pairs.countByStudyId(studyId) > 0) {
            throw new IllegalStateException("pairs were already formed for study " + studyId);
        }

        // Preregistered criterion of v1: same gender and same age band, different sitters.
        Map<String, List<TargetEntity>> buckets = new LinkedHashMap<>();
        for (TargetEntity target : targets.findByStudyIdOrderByCodeAsc(studyId)) {
            buckets.computeIfAbsent(target.gender().name() + "|" + target.ageBand().name(),
                    k -> new ArrayList<>()).add(target);
        }

        List<PairView> formed = new ArrayList<>();
        int unpaired = 0;
        for (List<TargetEntity> bucket : buckets.values()) {
            List<TargetEntity> remaining = new ArrayList<>(bucket);
            while (remaining.size() >= 2) {
                TargetEntity first = remaining.remove(0);
                int matchIndex = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (!remaining.get(i).sitterCode().equals(first.sitterCode())) {
                        matchIndex = i;
                        break;
                    }
                }
                if (matchIndex < 0) {
                    unpaired++;
                    continue;
                }
                TargetEntity second = remaining.remove(matchIndex);
                PairEntity pair = pairs.save(new PairEntity(studyId, Codes.of("PR"),
                        first.code(), second.code(), clock.instant()));
                formed.add(new PairView(pair.code(), pair.targetA(), pair.targetB()));
            }
            unpaired += remaining.size();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pairs", formed.size());
        payload.put("unpairedTargets", unpaired);
        payload.put("criterion", "SAME_GENDER_SAME_AGE_BAND_DIFFERENT_SITTER");
        auditTrail.append(AuditEventType.PAIRS_FORMED, ActorRole.SYSTEM, actorCode,
                "study:" + studyId, payload);

        return formed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PairView> pairs(long studyId) {
        List<PairView> out = new ArrayList<>();
        for (PairEntity pair : pairs.findByStudyIdOrderByCodeAsc(studyId)) {
            out.add(new PairView(pair.code(), pair.targetA(), pair.targetB()));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> codesWithRole(long studyId, ParticipantRole role) {
        List<String> out = new ArrayList<>();
        for (ParticipantRoleEntity entity : roles.findByStudyIdAndRoleAndActiveTrue(studyId, role)) {
            out.add(entity.code());
        }
        out.sort(String::compareTo);
        return out;
    }

    @Override
    @Transactional
    public void withdraw(String participantCode, String actorCode) {
        ParticipantEntity participant = participants.findByCode(participantCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown participant"));
        participant.erase(clock.instant());
        participants.save(participant);
        consents.findByParticipantCode(participantCode).forEach(c -> {
            c.revoke(clock.instant());
            consents.save(c);
        });
        roles.findByCode(participantCode).forEach(r -> {
            r.deactivate();
            roles.save(r);
        });

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("participant", participantCode);
        auditTrail.append(AuditEventType.PARTICIPANT_WITHDRAWN, ActorRole.RESEARCHER, actorCode,
                "participant:" + participantCode, payload);
    }

    private String prefixFor(ParticipantRole role) {
        return switch (role) {
            case RESEARCHER -> "RES";
            case EXPERIMENTER -> "EXP";
            case MEDIUM -> "MED";
            case REDACTOR -> "RED";
            case SITTER -> "SIT";
        };
    }
}
