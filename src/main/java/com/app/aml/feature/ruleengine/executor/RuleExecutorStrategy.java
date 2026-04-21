package com.app.aml.feature.ruleengine.executor;

import com.app.aml.feature.ruleengine.dto.RuleExecutionDto;
import java.util.Set;
import java.util.UUID;

public interface RuleExecutorStrategy {
    
    /**
     * Identifies which rule typology this strategy handles (e.g., "SMURFING").
     */
    String getRuleType();

    /**
     * Executes the rule logic against the database.
     * @return Set of account numbers that breached the rule.
     */
    Set<String> executeRule(RuleExecutionDto rule, UUID batchId);
}
