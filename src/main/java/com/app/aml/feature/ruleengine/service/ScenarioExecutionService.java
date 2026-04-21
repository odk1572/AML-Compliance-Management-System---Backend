package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.RuleExecutionDto;
import com.app.aml.feature.ruleengine.dto.ScenarioExecutionDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorFactory;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioExecutionService {

    private final RuleExecutorFactory ruleExecutorFactory;

    public void executeBatch(UUID batchId, ScenarioExecutionDto scenario) {
        
        if (scenario == null || scenario.getActiveRules() == null || scenario.getActiveRules().isEmpty()) {
            return; 
        }

        Set<String> finalAccounts = new HashSet<>();
        boolean isFirstRule = true;

        for (RuleExecutionDto rule : scenario.getActiveRules()) {
            
            // Get the right strategy dynamically
            RuleExecutorStrategy strategy = ruleExecutorFactory.getStrategy(rule.getRuleType());
            
            // Execute the rule to fetch the breaching accounts
            Set<String> currentAccounts = strategy.executeRule(rule, batchId);
            if (currentAccounts == null) {
                currentAccounts = new HashSet<>();
            }

            
            if (isFirstRule) {
                finalAccounts.addAll(currentAccounts);
                isFirstRule = false;
            } else {
                if ("OR".equalsIgnoreCase(scenario.getLogicalOperator())) {
                    finalAccounts.addAll(currentAccounts); 
                } else {
                    finalAccounts.retainAll(currentAccounts); 
                }
            }
        }

        // Generate the  alert

    }
}
