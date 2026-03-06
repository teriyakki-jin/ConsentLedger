package com.consentledger.domain.admin.service;

import com.consentledger.domain.admin.dto.AgentStatusUpdateRequest;
import com.consentledger.domain.admin.dto.AgentSummary;
import com.consentledger.domain.admin.dto.UserSummary;
import com.consentledger.domain.agent.entity.Agent;
import com.consentledger.domain.agent.entity.AgentStatus;
import com.consentledger.domain.agent.repository.AgentRepository;
import com.consentledger.domain.user.entity.User;
import com.consentledger.domain.user.repository.UserRepository;
import com.consentledger.fixture.AgentFixture;
import com.consentledger.fixture.UserFixture;
import com.consentledger.global.exception.BusinessException;
import com.consentledger.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AgentRepository agentRepository;

    @InjectMocks private AdminService adminService;

    @Test
    @DisplayName("전체 사용자 목록 반환")
    void listUsers_returnsAll() {
        User user1 = UserFixture.user();
        User user2 = UserFixture.admin();

        given(userRepository.findAll()).willReturn(List.of(user1, user2));

        List<UserSummary> result = adminService.listUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserSummary::getEmail)
                .containsExactlyInAnyOrder(user1.getEmail(), user2.getEmail());
    }

    @Test
    @DisplayName("사용자 없을 때 빈 목록 반환")
    void listUsers_emptyRepository_returnsEmpty() {
        given(userRepository.findAll()).willReturn(List.of());

        List<UserSummary> result = adminService.listUsers();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 Agent 목록 반환")
    void listAgents_returnsAll() {
        Agent agent1 = AgentFixture.active();
        Agent agent2 = AgentFixture.suspended();

        given(agentRepository.findAll()).willReturn(List.of(agent1, agent2));

        List<AgentSummary> result = adminService.listAgents();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Agent 없을 때 빈 목록 반환")
    void listAgents_emptyRepository_returnsEmpty() {
        given(agentRepository.findAll()).willReturn(List.of());

        List<AgentSummary> result = adminService.listAgents();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Agent 상태 변경 성공")
    void updateAgentStatus_success() {
        Agent agent = AgentFixture.active();
        AgentStatusUpdateRequest request = new AgentStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", AgentStatus.SUSPENDED);

        given(agentRepository.findById(agent.getId())).willReturn(Optional.of(agent));

        AgentSummary result = adminService.updateAgentStatus(agent.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(agent.getId());
        assertThat(result.getStatus()).isEqualTo(AgentStatus.SUSPENDED.name());
    }

    @Test
    @DisplayName("없는 Agent 상태 변경 시 AGENT_NOT_FOUND")
    void updateAgentStatus_agentNotFound_throws() {
        UUID unknownId = UUID.randomUUID();
        AgentStatusUpdateRequest request = new AgentStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", AgentStatus.SUSPENDED);

        given(agentRepository.findById(unknownId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateAgentStatus(unknownId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AGENT_NOT_FOUND);
    }
}
