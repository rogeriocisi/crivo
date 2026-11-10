package io.github.rogeriocisi.airlab.trials.internal;

import io.github.rogeriocisi.airlab.analysis.BinomialTest;
import io.github.rogeriocisi.airlab.audit.ActorRole;
import io.github.rogeriocisi.airlab.audit.AuditEventType;
import io.github.rogeriocisi.airlab.audit.AuditTrail;
import io.github.rogeriocisi.airlab.support.Codes;
import io.github.rogeriocisi.airlab.support.FieldCipher;
import io.github.rogeriocisi.airlab.support.Hashes;
import io.github.rogeriocisi.airlab.support.Seeds;
import io.github.rogeriocisi.airlab.trials.SessionOutcome;
import io.github.rogeriocisi.airlab.trials.SessionProgress;
import io.github.rogeriocisi.airlab.trials.SessionState;
import io.github.rogeriocisi.airlab.trials.TargetDerivation;
import io.github.rogeriocisi.airlab.trials.TargetPool;
import io.github.rogeriocisi.airlab.trials.TrialCandidates;
import io.github.rogeriocisi.airlab.trials.TrialOutcome;
import io.github.rogeriocisi.airlab.trials.TrialProtocol;
import io.github.rogeriocisi.airlab.trials.TrialSessionService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultTrialSessionService implements TrialSessionService {

    private static final int MAX_TRIALS = 200;
    private static final int MAX_IMPRESSION_CHARS = 4000;
    private static final String EXPLORATORY_REASON =
            "Dados de autoatendimento público: não se sabe quem testou, quantas vezes nem em que "
                    + "condições. São dados exploratórios e abertos, nunca confirmatórios. Um resultado "
                    + "confirmatório exige estudo pré-registrado, com participantes identificados e N "
                    + "declarado antes da coleta.";

    private final TestSessionRepository sessions;
    private final TestTrialRepository trials;
    private final AuditTrail auditTrail;
    private final FieldCipher cipher;
    private final Clock clock;

    DefaultTrialSessionService(TestSessionRepository sessions,
                               TestTrialRepository trials,
                               AuditTrail auditTrail,
                               FieldCipher cipher,
                               Clock clock) {
        this.sessions = sessions;
        this.trials = trials;
        this.auditTrail = auditTrail;
        this.cipher = cipher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SessionProgress start(TrialProtocol protocol, int declaredTrials) {
        if (declaredTrials < 1 || declaredTrials > MAX_TRIALS) {
            throw new IllegalArgumentException(
                    "a sessao precisa de 1 a " + MAX_TRIALS + " tentativas");
        }
        String seedHex = Seeds.newSeedHex();
        String commitment = commitment(seedHex, protocol, declaredTrials);

        TestSessionEntity session = sessions.save(new TestSessionEntity(
                Codes.of("TS"),
                protocol,
                declaredTrials,
                commitment,
                cipher.encrypt(seedHex),
                TargetPool.VERSION,
                now()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", session.code());
        payload.put("protocol", protocol.name());
        payload.put("declaredTrials", declaredTrials);
        payload.put("alternatives", protocol.alternatives());
        payload.put("seedCommitment", commitment);
        payload.put("targetPoolVersion", TargetPool.VERSION);
        auditTrail.append(AuditEventType.TEST_SESSION_OPENED, ActorRole.SYSTEM, null,
                "testSession:" + session.code(), payload);

        return progressOf(session);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionProgress progress(String sessionCode) {
        return progressOf(session(sessionCode));
    }

    @Override
    @Transactional
    public SessionProgress contributeEntropy(String sessionCode, String nonceHex) {
        TestSessionEntity session = activeSession(sessionCode);
        String nonce = normalisedNonce(nonceHex);

        session.contributeNonce(nonce);
        sessions.save(session);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", sessionCode);
        // The nonce is public from the start: it is not a secret, it only has to be
        // unpredictable to the server until it arrives.
        payload.put("participantNonce", nonce);
        payload.put("seedCommitment", session.seedCommitment());
        auditTrail.append(AuditEventType.TEST_ENTROPY_CONTRIBUTED, ActorRole.SYSTEM, null,
                "testSession:" + sessionCode, payload);

        return progressOf(session);
    }

    @Override
    @Transactional
    public TrialCandidates lockImpression(String sessionCode, String impression) {
        TestSessionEntity session = activeSession(sessionCode);
        if (!session.protocol().impressionRequired()) {
            throw new IllegalStateException(
                    "o protocolo " + session.protocol() + " nao usa impressao escrita");
        }
        if (impression == null || impression.isBlank()) {
            throw new IllegalArgumentException("a impressao nao pode ficar em branco");
        }
        if (impression.length() > MAX_IMPRESSION_CHARS) {
            throw new IllegalArgumentException(
                    "a impressao passa de " + MAX_IMPRESSION_CHARS + " caracteres");
        }

        requireEntropy(session);
        int index = nextTrialIndex(session);
        TestTrialEntity trial = trials.findBySessionCodeAndTrialIndex(sessionCode, index)
                .orElseGet(() -> trials.save(new TestTrialEntity(sessionCode, index)));
        if (trial.impressionLockedAt() != null) {
            throw new IllegalStateException(
                    "a impressao da tentativa " + index + " ja foi travada; ela nao pode ser "
                            + "reescrita depois de as candidatas aparecerem");
        }

        String text = impression.strip();
        List<TargetPool.Figure> candidates = candidateFigures(session, index);
        String slugs = String.join(",", candidates.stream().map(TargetPool.Figure::slug).toList());
        trial.lockImpression(text, Hashes.sha256Hex(text), slugs, now());
        trials.save(trial);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", sessionCode);
        payload.put("trial", index);
        // The impression itself stays out of the public trail: only its hash, which is what
        // proves it was not rewritten after the candidates were seen (P6, E12).
        payload.put("impressionSha256", trial.impressionSha256());
        auditTrail.append(AuditEventType.TEST_IMPRESSION_LOCKED, ActorRole.SYSTEM, null,
                "testSession:" + sessionCode, payload);

        return candidatesOf(session, trial);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialCandidates currentCandidates(String sessionCode) {
        TestSessionEntity session = activeSession(sessionCode);
        int index = nextTrialIndex(session);
        TestTrialEntity trial = trials.findBySessionCodeAndTrialIndex(sessionCode, index)
                .orElseThrow(() -> new IllegalStateException(
                        "a tentativa " + index + " ainda nao tem impressao travada"));
        if (trial.impressionLockedAt() == null) {
            throw new IllegalStateException(
                    "a tentativa " + index + " ainda nao tem impressao travada");
        }
        return candidatesOf(session, trial);
    }

    @Override
    @Transactional
    public SessionProgress submitChoice(String sessionCode, int chosenPosition) {
        TestSessionEntity session = activeSession(sessionCode);
        if (session.protocol().impressionRequired()) {
            throw new IllegalStateException(
                    "o protocolo " + session.protocol() + " espera uma ordenacao, nao uma escolha");
        }
        requireEntropy(session);
        requirePosition(session, chosenPosition);

        int index = nextTrialIndex(session);
        TestTrialEntity trial = trials.findBySessionCodeAndTrialIndex(sessionCode, index)
                .orElseGet(() -> trials.save(new TestTrialEntity(sessionCode, index)));

        lockAnswer(session, trial, chosenPosition, null, index);
        return progressOf(session);
    }

    @Override
    @Transactional
    public SessionProgress submitRanking(String sessionCode, List<Integer> rankedPositions) {
        TestSessionEntity session = activeSession(sessionCode);
        if (!session.protocol().impressionRequired()) {
            throw new IllegalStateException(
                    "o protocolo " + session.protocol() + " espera uma escolha, nao uma ordenacao");
        }
        requireEntropy(session);
        requirePermutation(session, rankedPositions);

        int index = nextTrialIndex(session);
        TestTrialEntity trial = trials.findBySessionCodeAndTrialIndex(sessionCode, index)
                .orElseThrow(() -> new IllegalStateException(
                        "a impressao da tentativa " + index + " precisa ser travada antes da ordenacao"));
        if (trial.impressionLockedAt() == null) {
            throw new IllegalStateException(
                    "a impressao da tentativa " + index + " precisa ser travada antes da ordenacao");
        }

        String ranking = String.join(",", rankedPositions.stream().map(String::valueOf).toList());
        lockAnswer(session, trial, rankedPositions.get(0), ranking, index);
        return progressOf(session);
    }

    @Override
    @Transactional
    public SessionOutcome finish(String sessionCode) {
        TestSessionEntity session = session(sessionCode);
        if (session.state() != SessionState.ACTIVE) {
            return outcomeOf(session);
        }
        long locked = trials.countBySessionCodeAndLockedAtIsNotNull(sessionCode);
        if (locked < session.declaredTrials()) {
            throw new IllegalStateException(
                    "faltam " + (session.declaredTrials() - locked) + " tentativa(s) para fechar a sessao; "
                            + "para encerrar antes do alvo declarado, registre a sessao como abandonada");
        }
        return close(session, SessionState.FINISHED, AuditEventType.TEST_SESSION_CLOSED);
    }

    @Override
    @Transactional
    public SessionOutcome abandon(String sessionCode) {
        TestSessionEntity session = session(sessionCode);
        if (session.state() != SessionState.ACTIVE) {
            return outcomeOf(session);
        }
        return close(session, SessionState.ABANDONED, AuditEventType.TEST_SESSION_ABANDONED);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionOutcome> outcome(String sessionCode) {
        TestSessionEntity session = session(sessionCode);
        return session.state() == SessionState.ACTIVE
                ? Optional.empty()
                : Optional.of(outcomeOf(session));
    }

    // --- internals ------------------------------------------------------------------------

    private SessionOutcome close(TestSessionEntity session, SessionState finalState,
                                 AuditEventType eventType) {
        String serverSeedHex = cipher.decrypt(session.seedEscrow());
        // A session abandoned before the participant contributed has no effective seed at all,
        // and saying so is more honest than inventing one.
        String seedHex = session.participantNonce() == null
                ? null
                : TargetDerivation.effectiveSeedHex(serverSeedHex, session.participantNonce());
        session.close(finalState, serverSeedHex, seedHex, now());
        sessions.save(session);

        SessionOutcome outcome = outcomeOf(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", session.code());
        payload.put("declaredTrials", session.declaredTrials());
        payload.put("lockedTrials", outcome.lockedTrials());
        payload.put("hits", outcome.hits());
        payload.put("serverSeedHex", serverSeedHex);
        payload.put("participantNonce", session.participantNonce());
        payload.put("seedHex", seedHex);
        payload.put("seedCommitment", session.seedCommitment());
        auditTrail.append(eventType, ActorRole.SYSTEM, null,
                "testSession:" + session.code(), payload);
        return outcome;
    }

    private void lockAnswer(TestSessionEntity session, TestTrialEntity trial, int chosenPosition,
                            String ranking, int index) {
        String seedHex = effectiveSeed(session);
        int targetPosition =
                TargetDerivation.targetIndex(seedHex, index, session.alternatives()) + 1;

        trial.lockAnswer(chosenPosition, ranking, targetPosition, now());
        trials.save(trial);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", session.code());
        payload.put("trial", index);
        payload.put("chosenPosition", chosenPosition);
        if (ranking != null) {
            payload.put("rankedPositions", ranking);
        }
        // Neither the target nor the hit goes in the trail here: the trail is public, and
        // publishing them trial by trial would leak the running score the session hides
        // (E10, lock 1). They become public with the seed, when the session closes.
        auditTrail.append(AuditEventType.TEST_TRIAL_LOCKED, ActorRole.SYSTEM, null,
                "testSession:" + session.code(), payload);
    }

    private SessionOutcome outcomeOf(TestSessionEntity session) {
        List<TestTrialEntity> all = trials.findBySessionCodeOrderByTrialIndexAsc(session.code());
        List<TrialOutcome> outcomes = new ArrayList<>();
        int hits = 0;
        int locked = 0;
        for (TestTrialEntity trial : all) {
            if (!trial.locked()) {
                continue;
            }
            locked++;
            if (Boolean.TRUE.equals(trial.hit())) {
                hits++;
            }
            outcomes.add(new TrialOutcome(
                    trial.trialIndex(),
                    trial.chosenPosition(),
                    trial.targetPosition(),
                    Boolean.TRUE.equals(trial.hit()),
                    slugList(trial.candidateSlugs()),
                    Optional.ofNullable(trial.impression()),
                    Optional.ofNullable(trial.impressionSha256())));
        }

        return new SessionOutcome(
                session.code(),
                session.protocol(),
                session.state(),
                session.declaredTrials(),
                locked,
                hits,
                1,
                session.alternatives(),
                BinomialTest.pValueGreaterOrEqual(hits, locked, 1, session.alternatives()),
                session.seedCommitment(),
                session.serverSeedHex(),
                session.participantNonce(),
                session.seedHex(),
                session.targetPoolVersion(),
                session.startedAt(),
                session.closedAt(),
                outcomes,
                EXPLORATORY_REASON);
    }

    private SessionProgress progressOf(TestSessionEntity session) {
        int locked = (int) trials.countBySessionCodeAndLockedAtIsNotNull(session.code());
        int next = Math.min(locked + 1, session.declaredTrials());
        boolean awaitingEntropy = session.participantNonce() == null
                && session.state() == SessionState.ACTIVE;
        boolean awaitingImpression = session.protocol().impressionRequired()
                && session.state() == SessionState.ACTIVE
                && trials.findBySessionCodeAndTrialIndex(session.code(), next)
                        .map(trial -> trial.impressionLockedAt() == null)
                        .orElse(true);

        return new SessionProgress(
                session.code(),
                session.protocol(),
                session.declaredTrials(),
                locked,
                next,
                session.seedCommitment(),
                session.startedAt(),
                session.state(),
                awaitingEntropy,
                awaitingImpression);
    }

    private TrialCandidates candidatesOf(TestSessionEntity session, TestTrialEntity trial) {
        List<TrialCandidates.Candidate> candidates = new ArrayList<>();
        List<String> slugs = slugList(trial.candidateSlugs());
        for (int i = 0; i < slugs.size(); i++) {
            TargetPool.Figure figure = figureBySlug(slugs.get(i));
            candidates.add(new TrialCandidates.Candidate(
                    i + 1, figure.slug(), figure.label(), figure.imagePath()));
        }
        return new TrialCandidates(session.code(), trial.trialIndex(),
                session.targetPoolVersion(), candidates);
    }

    private List<TargetPool.Figure> candidateFigures(TestSessionEntity session, int index) {
        String seedHex = effectiveSeed(session);
        List<Integer> poolIndices = TargetDerivation.candidatePoolIndices(
                seedHex, index, TargetPool.size(), session.alternatives());
        List<TargetPool.Figure> figures = new ArrayList<>();
        poolIndices.forEach(poolIndex -> figures.add(TargetPool.figure(poolIndex)));
        return figures;
    }

    private static TargetPool.Figure figureBySlug(String slug) {
        return TargetPool.figures().stream()
                .filter(figure -> figure.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "a figura " + slug + " nao esta no banco " + TargetPool.VERSION
                                + "; o banco e versionado justamente para isso nao acontecer"));
    }

    private static List<String> slugList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return List.of(commaSeparated.split(","));
    }

    /**
     * The seed every target comes from. It does not exist until the participant has
     * contributed, which is the whole point: before that moment nobody, this server included,
     * can know a single target of this session.
     */
    private String effectiveSeed(TestSessionEntity session) {
        if (session.participantNonce() == null) {
            throw new IllegalStateException(
                    "a sessao " + session.code() + " ainda nao recebeu a parte do participante");
        }
        return TargetDerivation.effectiveSeedHex(
                cipher.decrypt(session.seedEscrow()), session.participantNonce());
    }

    private static void requireEntropy(TestSessionEntity session) {
        if (session.participantNonce() == null) {
            throw new IllegalStateException(
                    "a sessao " + session.code() + " precisa da parte do participante antes da "
                            + "primeira tentativa");
        }
    }

    private static String normalisedNonce(String nonceHex) {
        String nonce = nonceHex == null ? "" : nonceHex.trim().toLowerCase();
        if (!nonce.matches("[0-9a-f]{32,64}")) {
            throw new IllegalArgumentException(
                    "a parte do participante precisa ter de 16 a 32 bytes em hexadecimal");
        }
        return nonce;
    }

    private int nextTrialIndex(TestSessionEntity session) {
        long locked = trials.countBySessionCodeAndLockedAtIsNotNull(session.code());
        if (locked >= session.declaredTrials()) {
            throw new IllegalStateException(
                    "a sessao ja tem as " + session.declaredTrials() + " tentativas declaradas");
        }
        return (int) locked + 1;
    }

    private void requirePosition(TestSessionEntity session, int position) {
        if (position < 1 || position > session.alternatives()) {
            throw new IllegalArgumentException(
                    "a posicao escolhida precisa estar entre 1 e " + session.alternatives());
        }
    }

    private void requirePermutation(TestSessionEntity session, List<Integer> positions) {
        if (positions == null || positions.size() != session.alternatives()
                || new LinkedHashSet<>(positions).size() != positions.size()) {
            throw new IllegalArgumentException(
                    "a ordenacao precisa listar as " + session.alternatives()
                            + " posicoes, cada uma uma vez");
        }
        positions.forEach(position -> requirePosition(session, position));
    }

    private TestSessionEntity session(String sessionCode) {
        return sessions.findByCode(sessionCode)
                .orElseThrow(() -> new IllegalArgumentException("sessao desconhecida"));
    }

    private TestSessionEntity activeSession(String sessionCode) {
        TestSessionEntity session = session(sessionCode);
        if (session.state() != SessionState.ACTIVE) {
            throw new IllegalStateException("a sessao " + sessionCode + " ja foi encerrada");
        }
        return session;
    }

    /**
     * Binds the seed to the parameters of the session, so the commitment published at the
     * start cannot be honoured by a session with a different number of trials or a different
     * target pool.
     */
    private static String commitment(String seedHex, TrialProtocol protocol, int declaredTrials) {
        return Hashes.sha256Hex(String.join("|",
                Arrays.asList(seedHex, protocol.name(), String.valueOf(declaredTrials),
                        String.valueOf(protocol.alternatives()), TargetPool.VERSION)));
    }

    private java.time.Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
