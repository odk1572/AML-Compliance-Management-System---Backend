package com.app.aml.feature.ruleengine.executor;

import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import java.util.Set;

public interface RuleExecutorStrategy {

    String getRuleType();

    Set<String> executeRule(RuleExecutionContextDto ruleContext);
}