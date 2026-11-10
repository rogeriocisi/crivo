package io.github.rogeriocisi.airlab.trials.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TestSessionRepository extends JpaRepository<TestSessionEntity, Long> {
    Optional<TestSessionEntity> findByCode(String code);
}

interface TestTrialRepository extends JpaRepository<TestTrialEntity, Long> {
    List<TestTrialEntity> findBySessionCodeOrderByTrialIndexAsc(String sessionCode);

    Optional<TestTrialEntity> findBySessionCodeAndTrialIndex(String sessionCode, int trialIndex);

    long countBySessionCodeAndLockedAtIsNotNull(String sessionCode);
}

final class TrialRepositories {
    private TrialRepositories() {
    }
}
