package com.consentledger.domain.audit.repository;

import com.consentledger.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT a FROM AuditLog a WHERE a.id > :afterId ORDER BY a.id ASC LIMIT :limit")
    List<AuditLog> findBatchAfter(@Param("afterId") Long afterId, @Param("limit") int limit);

    List<AuditLog> findByTsBetweenOrderByTsAsc(Instant from, Instant to);
}
