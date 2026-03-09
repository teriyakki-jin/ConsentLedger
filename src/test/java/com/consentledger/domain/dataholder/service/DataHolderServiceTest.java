package com.consentledger.domain.dataholder.service;

import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.dataholder.repository.DataHolderRepository;
import com.consentledger.fixture.ConsentFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DataHolderServiceTest {

    @Mock private DataHolderRepository dataHolderRepository;

    @InjectMocks private DataHolderService dataHolderService;

    @Test
    @DisplayName("list_all: 이름순으로 데이터 보유자 목록 반환")
    void listAll_returnsSortedSummaries() {
        DataHolder first = ConsentFixture.dataHolder();
        DataHolder second = DataHolder.builder()
                .id(first.getId())
                .institutionCode("BANK000")
                .name("Alpha Bank")
                .supportedMethods(List.of("PULL"))
                .build();
        given(dataHolderRepository.findAll()).willReturn(List.of(first, second));

        var result = dataHolderService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Bank");
    }
}
