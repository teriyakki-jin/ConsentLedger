package com.consentledger.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class TransferRequestControllerIntegrationTest extends BaseControllerIntegrationTest {

    private static final String USER_EMAIL = "user@consentledger.com";
    private static final String USER_PASSWORD = "user1234";

    private String userToken;
    private String consentId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        consentId = createConsentAndGetId(userToken);
    }

    @Test
    @DisplayName("POST /transfer-requests - 201 Created")
    void createTransferRequest_returns201() throws Exception {
        mockMvc.perform(post("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consentId", consentId,
                                "method", "PULL",
                                "idempotencyKey", "idem-" + UUID.randomUUID()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("GET /transfer-requests - 200 OK")
    void listTransferRequests_returns200() throws Exception {
        mockMvc.perform(get("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /transfer-requests - 201 Created (idempotency returns same record)")
    void createTransferRequest_sameIdempotencyKey_returnsExisting() throws Exception {
        String idempotencyKey = "idem-" + UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "consentId", consentId,
                "method", "PULL",
                "idempotencyKey", idempotencyKey
        );

        MvcResult first = mockMvc.perform(post("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request with same idempotency key - controller always returns 201
        MvcResult second = mockMvc.perform(post("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(extractDataField(first, "id")).isEqualTo(extractDataField(second, "id"));
    }

    @Test
    @DisplayName("POST /transfer-requests/{id}/approve - 200 OK")
    void approveTransferRequest_returns200() throws Exception {
        String transferId = createTransferAndGetId(consentId);

        mockMvc.perform(post("/transfer-requests/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /transfer-requests/{id}/execute - 200 OK (after approve)")
    void executeTransferRequest_returns200() throws Exception {
        String transferId = createTransferAndGetId(consentId);

        mockMvc.perform(post("/transfer-requests/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transfer-requests/" + transferId + "/execute")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /transfer-requests - 400 Bad Request (missing method)")
    void createTransferRequest_missingMethod_returns400() throws Exception {
        mockMvc.perform(post("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consentId", consentId
                                // method and idempotencyKey omitted
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transfer-requests - 403 Forbidden (no token)")
    void createTransferRequest_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/transfer-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consentId", consentId,
                                "method", "PULL",
                                "idempotencyKey", "idem-" + UUID.randomUUID()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /transfer-requests/{id}/approve - 409 Conflict (already approved)")
    void approveTransferRequest_alreadyApproved_returns409() throws Exception {
        String transferId = createTransferAndGetId(consentId);

        mockMvc.perform(post("/transfer-requests/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transfer-requests/" + transferId + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /transfer-requests/{id}/approve - 404 Not Found")
    void approveTransferRequest_notFound_returns404() throws Exception {
        mockMvc.perform(post("/transfer-requests/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String createTransferAndGetId(String consentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/transfer-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consentId", consentId,
                                "method", "PULL",
                                "idempotencyKey", "idem-" + UUID.randomUUID()
                        ))))
                .andReturn();

        return extractDataField(result, "id");
    }
}
