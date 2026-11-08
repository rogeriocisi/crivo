package io.github.rogeriocisi.airlab.audit.internal;

import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Optional<AuditEvent> findFirstByOrderBySeqDesc();

    @Query("select e from AuditEvent e order by e.seq asc")
    Stream<AuditEvent> streamAllOrdered();
}
