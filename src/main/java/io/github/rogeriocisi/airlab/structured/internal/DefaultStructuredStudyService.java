package io.github.rogeriocisi.airlab.structured.internal;

import io.github.rogeriocisi.airlab.allocation.AllocationService;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.catalog.Questionnaire;
import io.github.rogeriocisi.airlab.catalog.Questionnaires;
import io.github.rogeriocisi.airlab.participant.AccountService;
import io.github.rogeriocisi.airlab.participant.ConsultationRef;
import io.github.rogeriocisi.airlab.participant.ConsultationService;
import io.github.rogeriocisi.airlab.participant.ConsultationState;
import io.github.rogeriocisi.airlab.structured.AssignmentState;
import io.github.rogeriocisi.airlab.structured.CycleReport;
import io.github.rogeriocisi.airlab.structured.ReadingRequestExpired;
import io.github.rogeriocisi.airlab.structured.ReadingRequestSent;
import io.github.rogeriocisi.airlab.structured.StructuredReport;
import io.github.rogeriocisi.airlab.structured.StructuredProgress;
import io.github.rogeriocisi.airlab.structured.StructuredStudyService;
import io.github.rogeriocisi.airlab.structured.StructuredStudyTerms;
import io.github.rogeriocisi.airlab.structured.StructuredStudyView;
import io.github.rogeriocisi.airlab.study.PreregistrationSnapshot;
import io.github.rogeriocisi.airlab.study.StudyService;
import io.github.rogeriocisi.airlab.study.StudyStatus;
import io.github.rogeriocisi.airlab.study.StudySummary;
import io.github.rogeriocisi.airlab.support.CanonicalJson;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.Hashes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The P4 study and its automatic cycle (SPEC E13, phase C).
 *
 * <p>Every cycle operation is audited as {@code SYSTEM}, with no actor code, and none of them
 * writes a medium's code to the trail: the link between a pair and its medium stays in the
 * database until the unblinding.
 */
@Service
class DefaultStructuredStudyService implements StructuredStudyService {

    static final String DRAFT_SUFFIX = "-rascunho";
    private static final Set<StudyStatus> OPEN_STATUSES = Set.of(StudyStatus.PREREGISTERED, StudyStatus.IN_FIELD);

    private final StudyService studies;
    private final AllocationService allocation;
    private final ConsultationService consultations;
    private final AccountService accounts;
    private final StructuredStudyRepository structuredStudies;
    private final StructuredPairRepository pairs;
    private final ReadingAssignmentRepository assignments;
    private final StructuredReadingRepository readings;
    private final StructuredScoreRepository scores;
    private final StructuredReportRepository reports;
    private final StructuredUnblinding unblinding;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final boolean draftQuestionnaireAllowed;

    DefaultStructuredStudyService(StudyService studies,
                                  AllocationService allocation,
                                  ConsultationService consultations,
                                  AccountService accounts,
                                  StructuredStudyRepository structuredStudies,
                                  StructuredPairRepository pairs,
                                  ReadingAssignmentRepository assignments,
                                  StructuredReadingRepository readings,
                                  StructuredScoreRepository scores,
                                  StructuredReportRepository reports,
                                  StructuredUnblinding unblinding,
                                  AuditTrail auditTrail,
                                  ApplicationEventPublisher events,
                                  Clock clock,
                                  @Value("${airlab.p4.allow-draft-questionnaire:false}") boolean draftQuestionnaireAllowed) {
        this.studies = studies;
        this.allocation = allocation;
        this.consultations = consultations;
        this.accounts = accounts;
        this.structuredStudies = structuredStudies;
        this.pairs = pairs;
        this.assignments = assignments;
        this.readings = readings;
        this.scores = scores;
        this.reports = reports;
        this.unblinding = unblinding;
        this.auditTrail = auditTrail;
        this.events = events;
        this.clock = clock;
        this.draftQuestionnaireAllowed = draftQuestionnaireAllowed;
    }

    // --- The study ------------------------------------------------------------------------------

    @Override
    public void check(StructuredStudyTerms terms) {
        if (terms == null) {
            throw new IllegalArgumentException("Preencha o pré-registro.");
        }
        if (blank(terms.title()) || terms.title().strip().length() > 200) {
            throw new IllegalArgumentException("Dê um título ao estudo, com até 200 caracteres.");
        }
        if (terms.targetPairs() < 1 || terms.targetPairs() > StructuredStudyTerms.MAXIMUM_TARGET_PAIRS) {
            throw new IllegalArgumentException("O número de pares vai de 1 a "
                    + StructuredStudyTerms.MAXIMUM_TARGET_PAIRS + ".");
        }
        if (terms.maxPairsPerMedium() < 1 || terms.maxPairsPerMedium() > terms.targetPairs()) {
            throw new IllegalArgumentException("O teto de pares por médium vai de 1 ao número de pares (N).");
        }
        if (terms.deadlineDays() < 1 || terms.deadlineDays() > StructuredStudyTerms.MAXIMUM_DEADLINE_DAYS) {
            throw new IllegalArgumentException("O prazo do médium vai de 1 a "
                    + StructuredStudyTerms.MAXIMUM_DEADLINE_DAYS + " dias.");
        }
        if (!(terms.expectedEffect() > 0) || terms.expectedEffect() > 3) {
            throw new IllegalArgumentException("O efeito esperado (d) precisa ser maior que 0 e no máximo 3.");
        }
        if (blank(terms.hypothesis()) || terms.hypothesis().length() > 2000) {
            throw new IllegalArgumentException("Escreva a hipótese, com até 2000 caracteres.");
        }
        if (blank(terms.mediumInclusionCriteria()) || terms.mediumInclusionCriteria().length() > 2000) {
            throw new IllegalArgumentException("Escreva os critérios de inclusão de médiuns, com até 2000 caracteres.");
        }
    }

    @Override
    public boolean draftQuestionnaireAllowed() {
        return draftQuestionnaireAllowed;
    }

    @Override
    @Transactional
    public StructuredStudyView open(StructuredStudyTerms terms, String actorCode) {
        check(terms);
        if (openStudy().isPresent()) {
            throw new IllegalStateException("Já existe um estudo P4 aberto. Um estudo por vez.");
        }
        Questionnaire questionnaire = Questionnaires.current();
        if (questionnaire.version().endsWith(DRAFT_SUFFIX) && !draftQuestionnaireAllowed) {
            throw new IllegalStateException("O questionário " + questionnaire.version()
                    + " ainda é rascunho. Em produção, o estudo só congela a versão revisada.");
        }

        StudySummary study = studies.create(terms.title().strip(), PROTOCOL,
                StructuredPreregistration.plan(terms),
                StructuredPreregistration.annex(questionnaire, terms), actorCode);
        structuredStudies.save(new StructuredStudyEntity(study.id(), questionnaire.version(),
                terms.targetPairs(), terms.deadlineDays(), terms.maxPairsPerMedium(), clock.instant()));
        String commitment = allocation.commitSeed(study.id(), study.code());
        studies.freeze(study.id(), commitment, actorCode);

        // The requests already waiting are paired, and sent, at once.
        runCycle(clock.instant());
        return view(structuredStudies.findById(study.id()).orElseThrow(), studies.byId(study.id()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructuredStudyView> current() {
        return openStudy().map(open -> view(open.structured(), open.study()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredStudyView> concluded() {
        List<StructuredStudyView> out = new ArrayList<>();
        for (StructuredStudyEntity structured : structuredStudies.findAll()) {
            studies.byId(structured.studyId())
                    .filter(summary -> summary.status() == StudyStatus.UNBLINDED || summary.status() == StudyStatus.REPORTED)
                    .ifPresent(summary -> out.add(view(structured, summary)));
        }
        out.sort(Comparator.comparing(StructuredStudyView::frozenAt,
                Comparator.nullsLast(Comparator.<Instant>naturalOrder())).reversed());
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructuredStudyView> byCode(String studyCode) {
        return studies.byCode(studyCode)
                .flatMap(summary -> structuredStudies.findById(summary.id()).map(structured -> view(structured, summary)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructuredReport> report(String studyCode) {
        return studies.byCode(studyCode).flatMap(summary -> reports.findById(summary.id()).map(report -> {
            List<StructuredReport.PairResult> results = new ArrayList<>();
            for (StructuredScoreEntity score : scores.findByStudyIdOrderByPairCodeAsc(summary.id())) {
                results.add(new StructuredReport.PairResult(score.pairCode(), score.mediumCode(),
                        score.comparableCount(), score.sAA(), score.sAB(), score.sBB(), score.sBA(), score.d()));
            }
            return new StructuredReport(summary.code(), report.pairs(), new BigDecimal(report.primaryP()),
                    report.primaryMethod(), new BigDecimal(report.signP()), new BigDecimal(report.permutationP()),
                    report.sitterVerified(), report.sitterTotal(), report.readingVerified(), report.readingTotal(),
                    report.seedHex(), report.document(), report.documentSha256(), report.createdAt(), results);
        }));
    }

    private StructuredStudyView view(StructuredStudyEntity structured, StudySummary study) {
        PreregistrationSnapshot preregistration = studies.currentPreregistration(study.id()).orElseThrow();
        return new StructuredStudyView(study.id(), study.code(), study.title(), study.status(),
                structured.questionnaireVersion(), structured.targetPairs(), structured.deadlineDays(),
                structured.maxPairsPerMedium(), preregistration.sha256(), allocation.commitment(study.id()).orElse(""),
                preregistration.frozenAt().orElse(null), preregistration.document());
    }

    private record OpenStudy(StructuredStudyEntity structured, StudySummary study) {
    }

    private Optional<OpenStudy> openStudy() {
        for (StructuredStudyEntity structured : structuredStudies.findAll()) {
            Optional<StudySummary> study = studies.byId(structured.studyId())
                    .filter(summary -> OPEN_STATUSES.contains(summary.status()));
            if (study.isPresent()) {
                return Optional.of(new OpenStudy(structured, study.get()));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public StructuredProgress progress() {
        Optional<OpenStudy> open = openStudy();
        if (open.isEmpty()) {
            return StructuredProgress.none();
        }
        StructuredStudyEntity study = open.get().structured();
        long waiting = waitingRequests(study).size();

        List<StructuredPairEntity> all = pairs.findByStudyIdOrderByCodeAsc(study.studyId());
        List<String> codes = all.stream().map(StructuredPairEntity::code).toList();
        Map<String, List<ReadingAssignmentEntity>> history = new LinkedHashMap<>();
        List<String> assignmentCodes = new ArrayList<>();
        long expired = 0;
        long cancelled = 0;
        for (ReadingAssignmentEntity assignment : codes.isEmpty() ? List.<ReadingAssignmentEntity>of()
                : assignments.findByPairCodeInOrderByPairCodeAscAttemptAsc(codes)) {
            history.computeIfAbsent(assignment.pairCode(), key -> new ArrayList<>()).add(assignment);
            assignmentCodes.add(assignment.code());
            if (assignment.state() == AssignmentState.EXPIRED) {
                expired++;
            } else if (assignment.state() == AssignmentState.CANCELLED) {
                cancelled++;
            }
        }

        long active = 0;
        long waitingMedium = 0;
        long openAssignments = 0;
        long completed = 0;
        long dissolved = 0;
        for (StructuredPairEntity pair : all) {
            if (!pair.active()) {
                dissolved++;
                continue;
            }
            active++;
            List<ReadingAssignmentEntity> sent = history.getOrDefault(pair.code(), List.of());
            if (sent.stream().anyMatch(ReadingAssignmentEntity::open)) {
                openAssignments++;
            } else if (sent.stream().anyMatch(assignment -> assignment.state() == AssignmentState.COMPLETED)) {
                completed++;
            } else {
                waitingMedium++;
            }
        }
        long locked = assignmentCodes.isEmpty() ? 0 : readings.findByAssignmentCodeIn(assignmentCodes).size();
        return new StructuredProgress(study.targetPairs(), waiting, active, waitingMedium, openAssignments, completed,
                locked, expired, cancelled, dissolved);
    }

    // --- The cycle ------------------------------------------------------------------------------

    @Override
    @Transactional
    public CycleReport runCycle(Instant at) {
        Optional<OpenStudy> open = openStudy();
        if (open.isEmpty()) {
            return CycleReport.none();
        }
        // PostgreSQL keeps microseconds; truncating here keeps what is stored equal to what is used.
        Instant now = at.truncatedTo(ChronoUnit.MICROS);
        StructuredStudyEntity study = open.get().structured();
        int dissolved = dissolveWithdrawn(study, now);
        int expired = expireOverdue(study, now);
        int cancelled = cancelUnavailable(study, now);
        int formed = formPairs(study, now);
        int sent = sendPairs(study, now);
        // Phase D: at N, with both readings of every pair locked, the study unblinds itself.
        int unblinded = unblinding.unblindIfComplete(study, open.get().study(), now) ? 1 : 0;
        return new CycleReport(dissolved, expired, cancelled, formed, sent, unblinded);
    }

    /** Pairs with a withdrawn request are undone; the other request waits for a new pair. */
    private int dissolveWithdrawn(StructuredStudyEntity study, Instant now) {
        int dissolved = 0;
        for (StructuredPairEntity pair : pairs.findByStudyIdAndDissolvedAtIsNullOrderByCodeAsc(study.studyId())) {
            List<ConsultationRef> requests = consultations.requestsByCode(pair.requests());
            boolean withdrawn = requests.size() < 2
                    || requests.stream().anyMatch(request -> request.state() == ConsultationState.WITHDRAWN);
            if (!withdrawn) {
                continue;
            }
            for (ReadingAssignmentEntity assignment : assignments.findByPairCodeOrderByAttemptAsc(pair.code())) {
                if (assignment.open()) {
                    closeAssignment(assignment, AssignmentState.CANCELLED, now);
                    audit(AuditEventType.READING_REQUEST_CANCELLED, "assignment:" + assignment.code(),
                            Map.of("assignment", assignment.code(), "pair", pair.code(), "reason", "PAIR_DISSOLVED"));
                } else {
                    // A dissolved pair is out of the study: its medium stops seeing the names.
                    consultations.revokeFirstNames(assignment.code());
                }
            }
            pair.dissolve(now, StructuredPairEntity.DISSOLVED_BY_WITHDRAWAL);
            for (ConsultationRef request : requests) {
                if (request.state() != ConsultationState.WITHDRAWN && request.state() != ConsultationState.LOCKED) {
                    consultations.advance(request.code(), request.state(), ConsultationState.LOCKED);
                }
            }
            audit(AuditEventType.STRUCTURED_PAIR_DISSOLVED, "pair:" + pair.code(),
                    Map.of("pair", pair.code(), "reason", StructuredPairEntity.DISSOLVED_BY_WITHDRAWAL));
            dissolved++;
        }
        return dissolved;
    }

    /** Past the deadline: the request is closed as expired, and the pair goes back to the draw. */
    private int expireOverdue(StructuredStudyEntity study, Instant now) {
        int expired = 0;
        for (ReadingAssignmentEntity assignment : openAssignments(study)) {
            if (assignment.dueAt().isAfter(now)) {
                continue;
            }
            closeAssignment(assignment, AssignmentState.EXPIRED, now);
            backToPaired(assignment);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("assignment", assignment.code());
            payload.put("pair", assignment.pairCode());
            payload.put("attempt", assignment.attempt());
            audit(AuditEventType.READING_REQUEST_EXPIRED, "assignment:" + assignment.code(), payload);
            events.publishEvent(new ReadingRequestExpired(assignment.pairCode()));
            expired++;
        }
        return expired;
    }

    /** A medium who closed the account cannot answer: the pair goes back to the draw. */
    private int cancelUnavailable(StructuredStudyEntity study, Instant now) {
        Set<String> active = new HashSet<>(accounts.activeMediumCodes());
        int cancelled = 0;
        for (ReadingAssignmentEntity assignment : openAssignments(study)) {
            if (active.contains(assignment.mediumCode())) {
                continue;
            }
            closeAssignment(assignment, AssignmentState.CANCELLED, now);
            backToPaired(assignment);
            audit(AuditEventType.READING_REQUEST_CANCELLED, "assignment:" + assignment.code(),
                    Map.of("assignment", assignment.code(), "pair", assignment.pairCode(),
                            "reason", "MEDIUM_UNAVAILABLE"));
            cancelled++;
        }
        return cancelled;
    }

    private int formPairs(StructuredStudyEntity study, Instant now) {
        int room = study.targetPairs() - (int) pairs.countByStudyIdAndDissolvedAtIsNull(study.studyId());
        List<ConsultationRef> waiting = waitingRequests(study);
        // Phase E: two sitters flagged as possibly the same person never form a pair.
        Map<String, Set<String>> apart = accounts.keptApart(
                waiting.stream().map(ConsultationRef::sitterCode).distinct().toList());
        int formed = 0;
        for (StructuredPairing.Pairing pairing : StructuredPairing.form(waiting, room,
                (one, other) -> apart.getOrDefault(one, Set.of()).contains(other))) {
            String code = Codes.of("PAR");
            pairs.save(new StructuredPairEntity(code, study.studyId(), pairing.first().code(),
                    pairing.second().code(), now));
            consultations.advance(pairing.first().code(), ConsultationState.LOCKED, ConsultationState.PAIRED);
            consultations.advance(pairing.second().code(), ConsultationState.LOCKED, ConsultationState.PAIRED);
            // Codes only: the pairing attributes are the sitters' answers and stay off the trail.
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("pair", code);
            payload.put("requests", List.of(pairing.first().code(), pairing.second().code()));
            audit(AuditEventType.STRUCTURED_PAIR_FORMED, "pair:" + code, payload);
            formed++;
        }
        return formed;
    }

    private int sendPairs(StructuredStudyEntity study, Instant now) {
        List<String> activeMediums = accounts.activeMediumCodes();
        List<StructuredPairEntity> active = pairs.findByStudyIdAndDissolvedAtIsNullOrderByCodeAsc(study.studyId());
        // Phase E: pairs already received by each medium in this study, open and completed.
        Map<String, Integer> load = new HashMap<>();
        Map<String, List<ReadingAssignmentEntity>> histories = new HashMap<>();
        for (StructuredPairEntity pair : active) {
            List<ReadingAssignmentEntity> history = assignments.findByPairCodeOrderByAttemptAsc(pair.code());
            histories.put(pair.code(), history);
            history.stream().filter(assignment -> assignment.state() == AssignmentState.OPEN
                            || assignment.state() == AssignmentState.COMPLETED)
                    .forEach(assignment -> load.merge(assignment.mediumCode(), 1, Integer::sum));
        }
        Integer cap = study.maxPairsPerMedium();
        int sent = 0;
        for (StructuredPairEntity pair : active) {
            List<ReadingAssignmentEntity> history = histories.get(pair.code());
            if (history.stream().anyMatch(assignment -> assignment.state() == AssignmentState.OPEN
                    || assignment.state() == AssignmentState.COMPLETED)) {
                continue;
            }
            Set<String> earlier = new HashSet<>();
            history.forEach(assignment -> earlier.add(assignment.mediumCode()));
            List<String> sitters = consultations.requestsByCode(pair.requests()).stream()
                    .map(ConsultationRef::sitterCode).toList();
            Set<String> apart = new HashSet<>();
            accounts.keptApart(sitters).values().forEach(apart::addAll);

            List<String> pool = new ArrayList<>(new TreeSet<>(activeMediums));
            pool.removeAll(earlier);
            // Never a medium flagged with either sitter, never one at the cap. The pool recorded
            // below is this filtered one, so the draw can still be redone from it.
            pool.removeAll(apart);
            pool.removeIf(medium -> cap != null && load.getOrDefault(medium, 0) >= cap);
            if (pool.isEmpty()) {
                continue;
            }

            int attempt = history.size() + 1;
            String medium = allocation.drawStructuredMedium(study.studyId(), pair.code(), attempt, pool);
            List<String> order = allocation.structuredNameOrder(study.studyId(), pair.code(), attempt, pair.requests());
            String poolJson = CanonicalJson.write(pool);
            String code = Codes.of("ENV");
            Instant due = now.plus(Duration.ofDays(study.deadlineDays()));
            assignments.save(new ReadingAssignmentEntity(code, pair.code(), attempt, medium, poolJson,
                    Hashes.sha256Hex(poolJson), order.get(0), order.get(1), now, due));
            consultations.releaseFirstNames(code, medium, order);
            for (String request : pair.requests()) {
                consultations.advance(request, ConsultationState.PAIRED, ConsultationState.SENT_TO_MEDIUM);
            }

            // The pool goes in by hash and the order in clear; the medium does not go in at all.
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("assignment", code);
            payload.put("pair", pair.code());
            payload.put("attempt", attempt);
            payload.put("order", order);
            payload.put("eligibleCount", pool.size());
            payload.put("eligibleSha256", Hashes.sha256Hex(poolJson));
            payload.put("dueAt", due.toString());
            audit(AuditEventType.READING_REQUEST_SENT, "assignment:" + code, payload);
            events.publishEvent(new ReadingRequestSent(pair.code(), code, medium, due));
            load.merge(medium, 1, Integer::sum);
            sent++;
        }
        return sent;
    }

    private List<ConsultationRef> waitingRequests(StructuredStudyEntity study) {
        return consultations.requests(Set.of(ConsultationState.LOCKED)).stream()
                .filter(request -> request.questionnaireVersion().equals(study.questionnaireVersion()))
                .toList();
    }

    private List<ReadingAssignmentEntity> openAssignments(StructuredStudyEntity study) {
        Set<String> studyPairs = new HashSet<>();
        pairs.findByStudyIdAndDissolvedAtIsNullOrderByCodeAsc(study.studyId())
                .forEach(pair -> studyPairs.add(pair.code()));
        return assignments.findByStateOrderByCodeAsc(AssignmentState.OPEN).stream()
                .filter(assignment -> studyPairs.contains(assignment.pairCode()))
                .toList();
    }

    private void closeAssignment(ReadingAssignmentEntity assignment, AssignmentState state, Instant now) {
        assignment.close(state, now);
        consultations.revokeFirstNames(assignment.code());
    }

    private void backToPaired(ReadingAssignmentEntity assignment) {
        for (ConsultationRef request : consultations.requestsByCode(
                List.of(assignment.requestAt(1), assignment.requestAt(2)))) {
            if (request.state() == ConsultationState.SENT_TO_MEDIUM) {
                consultations.advance(request.code(), ConsultationState.SENT_TO_MEDIUM, ConsultationState.PAIRED);
            }
        }
    }

    private void audit(AuditEventType type, String subject, Map<String, Object> payload) {
        auditTrail.append(type, ActorRole.SYSTEM, null, subject, payload);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
