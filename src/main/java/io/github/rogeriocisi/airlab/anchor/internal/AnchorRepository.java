package io.github.rogeriocisi.airlab.anchor.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnchorRepository extends JpaRepository<AnchorEntity, Long> {

    Optional<AnchorEntity> findFirstByStatusOrderByIdDesc(String status);

    Optional<AnchorEntity> findFirstByOrderByIdDesc();

    List<AnchorEntity> findByStatusOrderByIdDesc(String status);

    Optional<AnchorEntity> findByCodeAndStatus(String code, String status);

    long countByStatus(String status);

    long countByStatusAndIdGreaterThan(String status, long id);
}
