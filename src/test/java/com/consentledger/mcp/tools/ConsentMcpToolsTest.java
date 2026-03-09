package com.consentledger.mcp.tools;

import com.consentledger.domain.consent.entity.Consent;
import com.consentledger.domain.consent.repository.ConsentRepository;
import com.consentledger.domain.dataholder.entity.DataHolder;
import com.consentledger.domain.user.entity.User;
import com.consentledger.fixture.ConsentFixture;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsentMcpToolsTest {

    @Mock private ConsentRepository consentRepository;

    @InjectMocks private ConsentMcpTools consentMcpTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(consentMcpTools, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("get_consents_by_user: 사용자 동의 목록 JSON 반환")
    void getConsentsByUser_returnsSerializedConsents() {
        UUID userId = UUID.randomUUID();
        User user = UserFixture.userWithId(userId);
        DataHolder dataHolder = ConsentFixture.dataHolder();
        Consent consent = ConsentFixture.active(user, dataHolder);
        given(consentRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(consent));

        String result = consentMcpTools.getConsentsByUser(userId.toString());

        verify(consentRepository).findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(result).contains(consent.getId().toString());
        assertThat(result).contains(userId.toString());
        assertThat(result).contains(dataHolder.getName());
    }

    @Test
    @DisplayName("get_consents_by_user: 잘못된 UUID면 에러 JSON 반환")
    void getConsentsByUser_invalidUuid_returnsErrorJson() {
        String result = consentMcpTools.getConsentsByUser("not-a-uuid");

        assertThat(result).isEqualTo("{\"error\": \"Invalid userId format. Must be a valid UUID.\"}");
    }

    @Test
    @DisplayName("get_consents_by_user: 저장소 예외 시 에러 JSON 반환")
    void getConsentsByUser_whenRepositoryFails_returnsErrorJson() {
        UUID userId = UUID.randomUUID();
        given(consentRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willThrow(new RuntimeException("DB unavailable"));

        String result = consentMcpTools.getConsentsByUser(userId.toString());

        assertThat(result).isEqualTo("{\"error\": \"Failed to retrieve consents.\"}");
    }
}
