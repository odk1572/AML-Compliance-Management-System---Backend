package com.app.aml.feature.ruleengine.dto.globalScenarioRules.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalScenarioRuleResponseDto {
    private UUID id;
    private UUID scenarioId;
    private UUID ruleId;

    @JsonProperty("isActive")
    private Boolean isActive;

    private Integer priorityOrder;

    // Converted to Instant for API consistency despite LocalDateTime in Entity
    private Instant sysCreatedAt;
}