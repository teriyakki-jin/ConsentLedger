package com.consentledger.mcp.tools;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.transfer.entity.TransferRequest;
import com.consentledger.domain.transfer.repository.TransferRequestRepository;
import com.consentledger.domain.user.entity.User;
import com.consentledger.fixture.ConsentFixture;
import com.consentledger.fixture.TransferRequestFixture;
import com.consentledger.fixture.UserFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferMcpToolsTest {

    @Mock private TransferRequestRepository transferRequestRepository;

    @InjectMocks private TransferMcpTools transferMcpTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(transferMcpTools, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("get_transfer_requests: 전송 요청 목록 JSON 반환")
    void getTransferRequests_returnsSerializedTransfers() {
        User requester = UserFixture.user();
        DataHolder dataHolder = ConsentFixture.dataHolder();
        Consent consent = ConsentFixture.active(requester, dataHolder);
        TransferRequest transfer = TransferRequestFixture.requested(consent, requester);
        given(transferRequestRepository.findAll()).willReturn(List.of(transfer));

        String result = transferMcpTools.getTransferRequests();

        verify(transferRequestRepository).findAll();
        assertThat(result).contains(transfer.getId().toString());
        assertThat(result).contains(consent.getId().toString());
        assertThat(result).contains("\"method\":\"PULL\"");
    }

    @Test
    @DisplayName("get_transfer_requests: 저장소 예외 시 에러 JSON 반환")
    void getTransferRequests_whenRepositoryFails_returnsErrorJson() {
        given(transferRequestRepository.findAll()).willThrow(new RuntimeException("DB unavailable"));

        String result = transferMcpTools.getTransferRequests();

        assertThat(result).isEqualTo("{\"error\": \"Failed to retrieve transfer requests.\"}");
    }
}
