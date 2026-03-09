package com.consentledger.domain.dataholder.dto;

import com.consentledger.domain.dataholder.entity.DataHolder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DataHolderSummary {

    private final UUID id;
    private final String institutionCode;
    private final String name;
    private final List<String> supportedMethods;

    public static DataHolderSummary from(DataHolder dataHolder) {
        return DataHolderSummary.builder()
                .id(dataHolder.getId())
                .institutionCode(dataHolder.getInstitutionCode())
                .name(dataHolder.getName())
                .supportedMethods(dataHolder.getSupportedMethods())
                .build();
    }
}
