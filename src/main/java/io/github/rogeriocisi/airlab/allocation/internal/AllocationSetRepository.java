package io.github.rogeriocisi.airlab.allocation.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AllocationSetRepository extends JpaRepository<AllocationSet, Long> {

    Optional<AllocationSet> findByStudyId(Long studyId);
}
