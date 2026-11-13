package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.analysis.BinomialTest;
import io.github.rogeriocisi.airlab.analysis.PermutationResult;
import io.github.rogeriocisi.airlab.analysis.StratifiedPermutationTest;
import io.github.rogeriocisi.airlab.analysis.WilcoxonResult;
import io.github.rogeriocisi.airlab.analysis.WilcoxonSignedRank;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.participant.RevealedAnswers;
import io.github.rogeriocisi.airlab.structured.AssignmentState;
import io.github.rogeriocisi.airlab.structured.ReadingCommitment;
import io.github.rogeriocisi.airlab.structured.StructuredReport;
import io.github.rogeriocisi.airlab.structured.StructuredScoring;
import io.github.rogeriocisi.airlab.structured.StructuredStudyReported;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudyStatus;
import io.github.rogeriocisi.airlab.study.StudySummary;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Hashes;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The P4 unblinding (SPEC E13, phase D): automatic, at N, irreversible, in one transaction.
 *
 * <p>The order matters and is the protocol's: close the collection, reveal the seed, mark the study
 * unblinded — and only then reveal the answers, check every commitment, score, analyse and write
 * the report. The database refuses a score for a study that is not unblinded, so no step here can
 * run early by mistake.
 */
@Component
class StructuredUnblinding {

    private final StudyService studies;
    private final AllocationService allocation;
    private final ConsultationService consultations;
    private final StructuredPairRepository pairs;
    private final ReadingAssignmentRepository assignments;
    private final StructuredReadingRepository readings;
    private final StructuredScoreRepository scores;
    private final StructuredReportRepository reports;
    private final FieldCipher cipher;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;

    StructuredUnblinding(StudyService studies, AllocationService allocation, ConsultationService consultations,
                         StructuredPairRepository pairs, ReadingAssignmentRepository assignments,
                         StructuredReadingRepository readings, StructuredScoreRepository scores,
                         StructuredReportRepository reports, FieldCipher cipher, AuditTrail auditTrail,
                         ApplicationEventPublisher events, EntityManager entityManager) {
        this.studies = studies;
        this.allocation = allocation;
        this.consultations = consultations;
        this.pairs = pairs;
        this.assignments = assignments;
        this.readings = readings;
        this.scores = scores;
        this.reports = reports;
        this.cipher = cipher;
        this.auditTrail = auditTrail;
        this.events = events;
        this.entityManager = entityManager;
    }

    /**
     * Unblinds the study when its N active pairs all have a completed request to a medium.
     * Called by the cycle, inside its transaction.
     *
     * @return whether the study was unblinded now
     */
    boolean unblindIfComplete(StructuredStudyEntity study, StudySummary summary, Instant now) {
        List<StructuredPairEntity> active = pairs.findByStudyIdAndDissolvedAtIsNullOrderByCodeAsc(study.studyId());
        if (active.size() < study.targetPairs()) {
            return false;
        }
        Map<String, ReadingAssignmentEntity> completed = new LinkedHashMap<>();
        for (StructuredPairEntity pair : active) {
            Optional<ReadingAssignmentEntity> done = assignments.findByPairCodeOrderByAttemptAsc(pair.code()).stream()
                    .filter(assignment -> assignment.state() == AssignmentState.COMPLETED)
                    .findFirst();
            if (done.isEmpty()) {
                return false;
            }
            completed.put(pair.code(), done.get());
        }
        unblind(study, summary, active, completed, now);
        return true;
    }

    private void unblind(StructuredStudyEntity study, StudySummary summary, List<StructuredPairEntity> active,
                         Map<String, ReadingAssignmentEntity> completed, Instant now) {
        long studyId = study.studyId();
        studies.advanceStatus(studyId, StudyStatus.SCORING_CLOSED, ActorRole.SYSTEM, null);
        String seed = allocation.revealSeed(studyId, ActorRole.SYSTEM, null);
        studies.advanceStatus(studyId, StudyStatus.UNBLINDED, ActorRole.SYSTEM, null);
        // The status must be in the database before the first score: its trigger checks it there.
        entityManager.flush();

        List<String> requestCodes = new ArrayList<>();
        active.forEach(pair -> requestCodes.addAll(pair.requests()));
        for (String code : requestCodes) {
            consultations.advance(code, ConsultationState.READING_RECEIVED, ConsultationState.UNBLINDED);
        }
        Map<String, RevealedAnswers> revealed = consultations.revealedAnswers(requestCodes);
        Questionnaire questionnaire = Questionnaires.byVersion(study.questionnaireVersion()).orElseThrow();

        // Every commitment is checked against what was revealed, before anything is scored.
        int sitterVerified = (int) revealed.values().stream().filter(RevealedAnswers::commitmentVerified).count();
        int readingVerified = 0;
        int readingTotal = 0;
        Map<String, Map<String, String>> readingFor = new TreeMap<>();
        for (ReadingAssignmentEntity assignment : completed.values()) {
            for (StructuredReadingEntity reading : readings.findByAssignmentCodeOrderBySlotAsc(assignment.code())) {
                Map<String, String> answers = DefaultMediumReadingService.decodeAnswers(cipher.decrypt(reading.answersEnc()));
                String recomputed = ReadingCommitment.of(study.questionnaireVersion(), reading.code(),
                        reading.requestCode(), answers, cipher.decrypt(reading.saltEnc()));
                readingTotal++;
                if (recomputed.equals(reading.commitment())) {
                    readingVerified++;
                }
                readingFor.put(reading.requestCode(), answers);
            }
        }

        List<StructuredReport.PairResult> results = new ArrayList<>();
        List<Double> differences = new ArrayList<>();
        for (StructuredPairEntity pair : active) {
            ReadingAssignmentEntity assignment = completed.get(pair.code());
            StructuredScoring.PairScore score = StructuredScoring.score(questionnaire,
                    answersOf(revealed, pair.requestA()), answersOf(revealed, pair.requestB()),
                    readingFor.getOrDefault(pair.requestA(), Map.of()), readingFor.getOrDefault(pair.requestB(), Map.of()));
            scores.save(new StructuredScoreEntity(studyId, pair.code(), assignment.code(), assignment.mediumCode(),
                    score.comparableFields(), score.targetMatchesA(), score.decoyMatchesA(), score.targetMatchesB(),
                    score.decoyMatchesB(), now));
            results.add(new StructuredReport.PairResult(pair.code(), assignment.mediumCode(),
                    score.comparableFields().size(), score.targetMatchesA(), score.decoyMatchesA(),
                    score.targetMatchesB(), score.decoyMatchesB(), score.d()));
            differences.add((double) score.d());
        }

        WilcoxonResult primary = WilcoxonSignedRank.greaterThanZero(differences);
        int positive = (int) differences.stream().filter(d -> d > 0).count();
        int nonZero = (int) differences.stream().filter(d -> d != 0).count();
        BigDecimal signP = BinomialTest.pValueGreaterOrEqual(positive, nonZero);
        PermutationResult permutation = permutation(questionnaire, active, revealed, readingFor, seed);

        List<StructuredPairEntity> all = pairs.findByStudyIdOrderByCodeAsc(studyId);
        long dissolved = all.stream().filter(pair -> !pair.active()).count();
        List<ReadingAssignmentEntity> sent = assignments.findByPairCodeInOrderByPairCodeAscAttemptAsc(
                all.stream().map(StructuredPairEntity::code).toList());
        long expired = sent.stream().filter(assignment -> assignment.state() == AssignmentState.EXPIRED).count();
        long cancelled = sent.stream().filter(assignment -> assignment.state() == AssignmentState.CANCELLED).count();

        String document = StructuredReportDocument.render(new StructuredReportDocument.Content(
                summary, study.questionnaireVersion(),
                studies.currentPreregistration(studyId).orElseThrow().sha256(),
                allocation.commitment(studyId).orElse(""), seed, results, primary, positive, nonZero, signP,
                permutation, sitterVerified, requestCodes.size(), readingVerified, readingTotal,
                dissolved, expired, cancelled));
        String documentSha256 = Hashes.sha256Hex(document);
        reports.save(new StructuredReportEntity(studyId, results.size(), plain(primary.pValue()), primary.method(),
                plain(signP), plain(permutation.pValue()), sitterVerified, requestCodes.size(), readingVerified,
                readingTotal, seed, document, documentSha256, now));

        Map<String, Object> unblinding = new LinkedHashMap<>();
        unblinding.put("study", summary.code());
        unblinding.put("pairs", results.size());
        unblinding.put("seedSha256", Hashes.sha256Hex(seed));
        unblinding.put("sitterCommitmentsVerified", sitterVerified + "/" + requestCodes.size());
        unblinding.put("readingCommitmentsVerified", readingVerified + "/" + readingTotal);
        auditTrail.append(AuditEventType.UNBLINDING_PERFORMED, ActorRole.SYSTEM, null, "study:" + studyId, unblinding);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("study", summary.code());
        analysis.put("primaryP", plain(primary.pValue()));
        analysis.put("signP", plain(signP));
        analysis.put("permutationP", plain(permutation.pValue()));
        auditTrail.append(AuditEventType.ANALYSIS_RUN, ActorRole.SYSTEM, null, "study:" + studyId, analysis);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("study", summary.code());
        report.put("documentSha256", documentSha256);
        auditTrail.append(AuditEventType.REPORT_GENERATED, ActorRole.SYSTEM, null, "study:" + studyId, report);

        studies.advanceStatus(studyId, StudyStatus.REPORTED, ActorRole.SYSTEM, null);

        List<String> sitters = consultations.requestsByCode(requestCodes).stream()
                .map(ConsultationRef::sitterCode).sorted().toList();
        List<String> mediums = new ArrayList<>(new TreeSet<>(
                completed.values().stream().map(ReadingAssignmentEntity::mediumCode).toList()));
        events.publishEvent(new StructuredStudyReported(summary.code(), sitters, mediums));
    }

    /**
     * The permutation matrix: one row per sitter, one column per reading, in pair order (A, B); the
     * reading on the same index is the one made for that sitter's target. Readings move only within
     * the stratum of the pairing.
     */
    private PermutationResult permutation(Questionnaire questionnaire, List<StructuredPairEntity> active,
                                          Map<String, RevealedAnswers> revealed,
                                          Map<String, Map<String, String>> readingFor, String seed) {
        List<String> order = new ArrayList<>();
        active.forEach(pair -> order.addAll(pair.requests()));
        Map<String, String> strata = new TreeMap<>();
        consultations.requestsByCode(order).forEach(request -> strata.put(request.code(), request.stratum()));
        List<String> strataIndex = new ArrayList<>(new TreeSet<>(strata.values()));

        int n = order.size();
        double[][] matrix = new double[n][n];
        int[] stratum = new int[n];
        for (int i = 0; i < n; i++) {
            stratum[i] = strataIndex.indexOf(strata.get(order.get(i)));
            Map<String, String> sitter = answersOf(revealed, order.get(i));
            for (int j = 0; j < n; j++) {
                matrix[i][j] = StructuredScoring.answeredMatches(questionnaire,
                        readingFor.getOrDefault(order.get(j), Map.of()), sitter);
            }
        }
        return StratifiedPermutationTest.diagonalExceedsChance(matrix, stratum, seed);
    }

    private static Map<String, String> answersOf(Map<String, RevealedAnswers> revealed, String requestCode) {
        RevealedAnswers answers = revealed.get(requestCode);
        return answers == null ? Map.of() : answers.answers();
    }

    static String plain(BigDecimal value) {
        return value.setScale(6, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}
