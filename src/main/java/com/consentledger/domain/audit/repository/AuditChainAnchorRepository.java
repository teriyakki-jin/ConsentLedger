package com.consentledger.domain.audit.repository;

import com.consentledger.domain.audit.entity.AuditChainAnchor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuditChainAnchorRepository extends JpaRepository<AuditChainAnchor, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuditChainAnchor a WHERE a.id = 1")
    Optional<AuditChainAnchor> findAnchorForUpdate();
}
