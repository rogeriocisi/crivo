package io.github.rogeriocisi.airlab.participant.internal;

import io.github.rogeriocisi.airlab.participant.ParticipantRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ParticipantEntityRepository extends JpaRepository<ParticipantEntity, Long> {
    Optional<ParticipantEntity> findByCode(String code);

    List<ParticipantEntity> findByContactFingerprint(String contactFingerprint);
}

interface TargetDetailRepository extends JpaRepository<TargetDetailEntity, Long> {
    Optional<TargetDetailEntity> findByTargetCode(String targetCode);
}

interface ConsentRepository extends JpaRepository<ConsentEntity, Long> {
    List<ConsentEntity> findByParticipantCode(String participantCode);
}

interface ParticipantRoleRepository extends JpaRepository<ParticipantRoleEntity, Long> {
    List<ParticipantRoleEntity> findByStudyIdAndRoleAndActiveTrue(Long studyId, ParticipantRole role);

    List<ParticipantRoleEntity> findByCode(String code);

    List<ParticipantRoleEntity> findByStudyIdAndCodeIn(Long studyId, List<String> codes);
}

interface TargetRepository extends JpaRepository<TargetEntity, Long> {
    List<TargetEntity> findByStudyIdOrderByCodeAsc(Long studyId);

    Optional<TargetEntity> findByCode(String code);
}

interface PairRepository extends JpaRepository<PairEntity, Long> {
    List<PairEntity> findByStudyIdOrderByCodeAsc(Long studyId);

    long countByStudyId(Long studyId);
}

final class ParticipantRepositories {
    private ParticipantRepositories() {
    }
}
