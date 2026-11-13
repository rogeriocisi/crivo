package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.structured.AssignmentState;
import io.github.rogeriocisi.airlab.structured.FeedbackService;
import io.github.rogeriocisi.airlab.structured.ReadingFeedback;
import io.github.rogeriocisi.airlab.structured.TargetFeedback;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudySummary;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The P4 feedback (SPEC E13, phase D). Everything here starts from a score row, and score rows exist
 * only after the unblinding — the database refuses them before — so there is no "too early" branch
 * to get wrong: before the unblinding, there is simply nothing to build from.
 */
@Service
class DefaultFeedbackService implements FeedbackService {

    private final StructuredPairRepository pairs;
    private final StructuredScoreRepository scores;
    private final StructuredReadingRepository readings;
    private final ReadingAssignmentRepository assignments;
    private final StructuredStudyRepository structuredStudies;
    private final StudyService studies;
    private final ConsultationService consultations;
    private final FieldCipher cipher;

    DefaultFeedbackService(StructuredPairRepository pairs, StructuredScoreRepository scores,
                           StructuredReadingRepository readings, ReadingAssignmentRepository assignments,
                           StructuredStudyRepository structuredStudies, StudyService studies,
                           ConsultationService consultations, FieldCipher cipher) {
        this.pairs = pairs;
        this.scores = scores;
        this.readings = readings;
        this.assignments = assignments;
        this.structuredStudies = structuredStudies;
        this.studies = studies;
        this.consultations = consultations;
        this.cipher = cipher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TargetFeedback> forRequest(String requestCode) {
        for (StructuredPairEntity pair : pairs.findActiveByRequest(requestCode)) {
            Optional<StructuredScoreEntity> scored = scores.findByPairCode(pair.code());
            if (scored.isEmpty()) {
                continue;
            }
            StructuredScoreEntity score = scored.get();
            boolean isA = pair.requestA().equals(requestCode);
            Optional<StructuredReadingEntity> reading = readings.findByAssignmentCodeOrderBySlotAsc(score.assignmentCode())
                    .stream().filter(candidate -> candidate.requestCode().equals(requestCode)).findFirst();
            if (reading.isEmpty()) {
                continue;
            }
            StructuredStudyEntity study = structuredStudies.findById(pair.studyId()).orElseThrow();
            return Optional.of(new TargetFeedback(studyCode(pair.studyId()), study.questionnaireVersion(),
                    DefaultMediumReadingService.decodeAnswers(cipher.decrypt(reading.get().answersEnc())),
                    score.comparableFields(),
                    isA ? score.sAA() : score.sBB(),
                    isA ? score.sAB() : score.sBA()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingFeedback> forMedium(String mediumCode) {
        List<ReadingAssignmentEntity> done = assignments.findByMediumCodeAndStateInOrderByAssignedAtDescCodeAsc(
                mediumCode, EnumSet.of(AssignmentState.COMPLETED));
        Map<String, StructuredScoreEntity> byAssignment = new java.util.HashMap<>();
        if (!done.isEmpty()) {
            scores.findByAssignmentCodeIn(done.stream().map(ReadingAssignmentEntity::code).toList())
                    .forEach(score -> byAssignment.put(score.assignmentCode(), score));
        }
        List<ReadingFeedback> out = new ArrayList<>();
        for (ReadingAssignmentEntity assignment : done) {
            StructuredScoreEntity score = byAssignment.get(assignment.code());
            if (score == null) {
                continue;
            }
            StructuredPairEntity pair = pairs.findByCode(assignment.pairCode()).orElseThrow();
            Map<String, String> names = consultations.releasedFirstNames(mediumCode, assignment.code());
            String study = studyCode(pair.studyId());
            for (int position = 1; position <= 2; position++) {
                String request = assignment.requestAt(position);
                boolean isA = pair.requestA().equals(request);
                out.add(new ReadingFeedback(study, assignment.code(), position, names.getOrDefault(request, "—"),
                        score.comparableCount(),
                        isA ? score.sAA() : score.sBB(),
                        isA ? score.sAB() : score.sBA(),
                        score.d()));
            }
        }
        return out;
    }

    private String studyCode(Long studyId) {
        return studies.byId(studyId).map(StudySummary::code).orElse("");
    }
}
