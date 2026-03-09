package com.consentledger.domain.audit.anomaly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// [HIGH-1] Immutable DTO: @Builder with @JsonDeserialize for Jackson compatibility
@Getter
@Builder
@JsonDeserialize(builder = AnomalyFinding.AnomalyFindingBuilder.class)
public class AnomalyFinding {

    private final String patternType;
    private final String severity;
    private final String description;
    private final String affectedActor;
    private final List<Long> evidenceLogIds;
    private final String recommendation;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class AnomalyFindingBuilder {
        // Lombok generates builder methods; @JsonPOJOBuilder tells Jackson
        // to use method names without prefix (e.g., "patternType" not "withPatternType")
    }
}
