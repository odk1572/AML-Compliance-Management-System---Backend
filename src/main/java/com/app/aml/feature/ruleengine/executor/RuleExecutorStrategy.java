package com.app.aml.feature.ruleengine.executor;

import java.util.Set;
import java.util.UUID;

public interface RuleExecutorStrategy {

    String getRuleType();

    Set<String> executeRule(RuleExecutionDto rule, UUID batchId);
}
