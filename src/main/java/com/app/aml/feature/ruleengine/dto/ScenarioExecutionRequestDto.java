package com.app.aml.feature.ruleengine.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ScenarioExecutionRequestDto {
    // e.g., "2026-01-01T00:00:00Z"
    private Instant globalLookbackStart;
    // e.g., "2026-01-31T23:59:59Z"
    private Instant globalLookbackEnd;
}