package com.consentledger.domain.dataholder.service;

import com.consentledger.domain.dataholder.dto.DataHolderSummary;
import com.consentledger.domain.dataholder.repository.DataHolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataHolderService {

    private final DataHolderRepository dataHolderRepository;

    @Transactional(readOnly = true)
    public List<DataHolderSummary> listAll() {
        return dataHolderRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(DataHolderSummary::from)
                .toList();
    }
}
