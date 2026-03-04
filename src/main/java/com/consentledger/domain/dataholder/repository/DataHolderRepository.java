package com.consentledger.domain.dataholder.repository;

import com.consentledger.domain.dataholder.entity.DataHolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DataHolderRepository extends JpaRepository<DataHolder, UUID> {

    Optional<DataHolder> findByInstitutionCode(String institutionCode);
}
