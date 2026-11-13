package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.structured.AssignmentState;
import io.github.rogeriocisi.airlab.structured.MediumAssignment;
import io.github.rogeriocisi.airlab.structured.MediumReadingService;
import io.github.rogeriocisi.airlab.structured.PairReadingsReceived;
import io.github.rogeriocisi.airlab.structured.ReadingCommitment;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Seeds;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The medium's requests and readings (SPEC E13, phase C).
 *
 * <p>Every lookup starts from the medium's own code, and the names come from {@code participant},
 * which hands over only what was released to that medium for that request. A reading is stored
 * once, already locked, and the trail gets its salted commitment without the medium's code.
 */
@Service
class DefaultMediumReadingService implements MediumReadingService {

    private static final EnumSet<AssignmentState> VISIBLE = EnumSet.of(AssignmentState.OPEN, AssignmentState.COMPLETED);
    private static final EnumSet<AssignmentState> CLOSED_WITHOUT_READING =
            EnumSet.of(AssignmentState.EXPIRED, AssignmentState.CANCELLED);

    private final ReadingAssignmentRepository assignments;
    private final StructuredReadingRepository readings;
    private final StructuredPairRepository pairs;
    private final StructuredStudyRepository studies;
    private final ConsultationService consultations;
    private final FieldCipher cipher;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    DefaultMediumReadingService(ReadingAssignmentRepository assignments,
                                StructuredReadingRepository readings,
                                StructuredPairRepository pairs,
                                StructuredStudyRepository studies,
                                ConsultationService consultations,
                                FieldCipher cipher,
                                AuditTrail auditTrail,
                                ApplicationEventPublisher events,
                                Clock clock) {
        this.assignments = assignments;
        this.readings = readings;
        this.pairs = pairs;
        this.studies = studies;
        this.consultations = consultations;
        this.cipher = cipher;
        this.auditTrail = auditTrail;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediumAssignment> assignments(String mediumCode) {
        List<MediumAssignment> out = new ArrayList<>();
        for (ReadingAssignmentEntity assignment
                : assignments.findByMediumCodeAndStateInOrderByAssignedAtDescCodeAsc(mediumCode, VISIBLE)) {
            view(mediumCode, assignment).ifPresent(out::add);
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediumAssignment> assignment(String mediumCode, String assignmentCode) {
        return assignments.findByCode(assignmentCode)
                .filter(assignment -> assignment.mediumCode().equals(mediumCode))
                .filter(assignment -> VISIBLE.contains(assignment.state()))
                .flatMap(assignment -> view(mediumCode, assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public long closedWithoutReading(String mediumCode) {
        return assignments.countByMediumCodeAndStateIn(mediumCode, CLOSED_WITHOUT_READING);
    }

    /**
     * The medium's view of a request, or nothing when a name is no longer released to them — a
     * withdrawn sitter, a dissolved pair — so a half-revoked request never shows up half-empty.
     */
    private Optional<MediumAssignment> view(String mediumCode, ReadingAssignmentEntity assignment) {
        Map<String, String> names = consultations.releasedFirstNames(mediumCode, assignment.code());
        Map<Integer, StructuredReadingEntity> locked = new TreeMap<>();
        readings.findByAssignmentCodeOrderBySlotAsc(assignment.code()).forEach(reading -> locked.put(reading.slot(), reading));

        List<MediumAssignment.Slot> slots = new ArrayList<>();
        for (int position = 1; position <= 2; position++) {
            String name = names.get(assignment.requestAt(position));
            if (name == null) {
                return Optional.empty();
            }
            StructuredReadingEntity reading = locked.get(position);
            slots.add(new MediumAssignment.Slot(position, name, reading != null,
                    reading == null ? null : reading.lockedAt()));
        }
        return Optional.of(new MediumAssignment(assignment.code(), assignment.state(), questionnaireVersion(assignment),
                assignment.assignedAt(), assignment.dueAt(), slots));
    }

    private String questionnaireVersion(ReadingAssignmentEntity assignment) {
        return pairs.findByCode(assignment.pairCode())
                .flatMap(pair -> studies.findById(pair.studyId()))
                .map(StructuredStudyEntity::questionnaireVersion)
                .orElseThrow(() -> new IllegalStateException("request " + assignment.code() + " has no P4 study"));
    }

    @Override
    public void check(String questionnaireVersion, Map<String, String> answers) {
        Questionnaires.byVersion(questionnaireVersion)
                .orElseThrow(() -> new IllegalStateException("unknown questionnaire " + questionnaireVersion))
                .checkMediumAnswers(answers);
    }

    @Override
    @Transactional
    public void lock(String mediumCode, String assignmentCode, int position, Map<String, String> answers) {
        ReadingAssignmentEntity assignment = assignments.findByCode(assignmentCode)
                .filter(candidate -> candidate.mediumCode().equals(mediumCode))
                .orElseThrow(() -> new IllegalStateException("this request was not sent to this medium"));
        if (!assignment.open()) {
            throw new IllegalStateException("this request is no longer open");
        }
        // PostgreSQL keeps microseconds: truncate here, so what is stored is what was used.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (!now.isBefore(assignment.dueAt())) {
            throw new IllegalStateException("the deadline of this request has passed");
        }
        if (position != 1 && position != 2) {
            throw new IllegalArgumentException("a pair has positions 1 and 2");
        }
        String request = assignment.requestAt(position);
        if (!consultations.releasedFirstNames(mediumCode, assignmentCode).containsKey(request)) {
            throw new IllegalStateException("no first name is released to this medium for this position");
        }
        List<StructuredReadingEntity> existing = readings.findByAssignmentCodeOrderBySlotAsc(assignmentCode);
        if (existing.stream().anyMatch(reading -> reading.slot() == position)) {
            throw new IllegalStateException("this reading is already locked");
        }
        String version = questionnaireVersion(assignment);
        check(version, answers);

        Map<String, String> sorted = new TreeMap<>(answers);
        String code = Codes.of("LEI");
        String salt = Seeds.newSeedHex();
        String commitment = ReadingCommitment.of(version, code, request, sorted, salt);
        readings.save(new StructuredReadingEntity(code, assignmentCode, position, request, version,
                cipher.encrypt(encodeAnswers(sorted)), cipher.encrypt(salt), commitment, now));

        // No actor code: which medium reads which pair stays blind until the unblinding (P4 matrix).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reading", code);
        payload.put("assignment", assignmentCode);
        payload.put("position", position);
        payload.put("commitment", commitment);
        auditTrail.append(AuditEventType.STRUCTURED_READING_LOCKED, ActorRole.MEDIUM, null,
                "reading:" + code, payload);

        if (existing.size() + 1 == 2) {
            assignment.close(AssignmentState.COMPLETED, now);
            for (int slot = 1; slot <= 2; slot++) {
                consultations.advance(assignment.requestAt(slot), ConsultationState.SENT_TO_MEDIUM,
                        ConsultationState.READING_RECEIVED);
            }
            events.publishEvent(new PairReadingsReceived(assignment.pairCode()));
        }
    }

    /** Same stored form as the sitter's answers: {@code key=option} lines, sorted by key. */
    static String encodeAnswers(Map<String, String> answers) {
        StringBuilder out = new StringBuilder();
        new TreeMap<>(answers).forEach((key, value) -> out.append(key).append('=').append(value).append('\n'));
        return out.toString();
    }

    static Map<String, String> decodeAnswers(String encoded) {
        Map<String, String> answers = new TreeMap<>();
        for (String line : encoded.split("\n")) {
            if (!line.isEmpty()) {
                int equals = line.indexOf('=');
                answers.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        return answers;
    }
}
