package com.consentledger.domain.transfer.repository;

import com.consentledger.domain.transfer.entity.TransferRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, UUID> {

    Optional<TransferRequest> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransferRequest t WHERE t.id = :id")
    Optional<TransferRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT tr FROM TransferRequest tr
        WHERE tr.requesterUser.id = :userId
           OR tr.consent.user.id = :userId
        ORDER BY tr.createdAt DESC
        """)
    List<TransferRequest> findByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT tr FROM TransferRequest tr
        WHERE tr.requesterAgent.id = :agentId
        ORDER BY tr.createdAt DESC
        """)
    List<TransferRequest> findByAgentId(@Param("agentId") UUID agentId);
}
