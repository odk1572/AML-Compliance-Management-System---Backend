package com.app.aml.feature.ruleengine.executor;

import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import java.util.List;

public interface RuleExecutorStrategy {

    String getRuleType();

    List<RuleBreachResult> executeRule(RuleExecutionContextDto ruleContext);
}