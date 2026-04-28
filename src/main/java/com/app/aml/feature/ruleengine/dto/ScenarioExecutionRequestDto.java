package com.app.aml.feature.ruleengine.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ScenarioExecutionRequestDto {

    private Instant referenceTime;
    private Instant globalLookbackStart;
}