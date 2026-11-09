package io.github.rogeriocisi.airlab.study.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface StudyRepository extends JpaRepository<StudyEntity, Long> {

    Optional<StudyEntity> findByCode(String code);

    List<StudyEntity> findAllByOrderByCreatedAtDesc();
}

interface PreregistrationVersionRepository extends JpaRepository<PreregistrationVersionEntity, Long> {

    Optional<PreregistrationVersionEntity> findFirstByStudyIdOrderByVersionDesc(Long studyId);

    List<PreregistrationVersionEntity> findByStudyIdOrderByVersionAsc(Long studyId);
}

interface ProtocolDeviationRepository extends JpaRepository<ProtocolDeviationEntity, Long> {

    List<ProtocolDeviationEntity> findByStudyIdOrderByOccurredAtAsc(Long studyId);
}

/** Holder so the repositories can share one file; never instantiated. */
final class StudyRepositories {

    private StudyRepositories() {
    }
}
