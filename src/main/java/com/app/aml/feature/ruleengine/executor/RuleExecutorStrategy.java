package com.app.aml.feature.ruleengine.executor;

import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import java.util.Set;
import java.util.UUID;

public interface RuleExecutorStrategy {

    String getRuleType();

    Set<UUID> executeRule(RuleExecutionContextDto ruleContext);
}