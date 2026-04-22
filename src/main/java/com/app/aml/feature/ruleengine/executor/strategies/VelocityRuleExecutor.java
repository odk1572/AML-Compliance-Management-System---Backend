package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityRuleExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "VELOCITY";
    }

    @Override
    public Set<String> executeRule(RuleExecutionDto rule, UUID batchId) {
        log.info("Executing Velocity Rule [{}] for Batch [{}]", rule.getRuleCode(), batchId);

        
        int countThreshold = 0; 
        String lookbackInterval = null; 

        // Extract dynamic DB configurations (Global + Tenant overrides)
        if (rule.getConditions() != null) {
            for (ConditionExecutionDto cond : rule.getConditions()) {
                if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) {
                    countThreshold = Integer.parseInt(cond.getThresholdValue());
                }
                
                if (cond.getLookbackPeriod() != null) {
                    lookbackInterval = cond.getLookbackPeriod().replace("h", " hours").replace("d", " days");
                }
            }
        }

        // Ensure the Service Layer did its job of loading Global/Tenant data
        if (countThreshold == 0 || lookbackInterval == null) {
            log.error("Rule configuration incomplete for RuleCode: {}. Cannot execute.", rule.getRuleCode());
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Velocity Rule.");
        }

        // Execute Velocity Query: High transaction count over a short window
        String sql = """
            SELECT originator_account_no
            FROM transactions
            WHERE batch_id = ?
              AND transaction_timestamp >= (
                  SELECT MAX(transaction_timestamp) 
                  FROM transactions 
                  WHERE batch_id = ?
              ) - CAST(? as INTERVAL)
            GROUP BY originator_account_no
            HAVING COUNT(id) >= ?    
        """;

        List<String> results = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getString("originator_account_no"),
            batchId,             
            batchId,             
            lookbackInterval,    
            countThreshold       
        );

        return new HashSet<>(results);
    }
}
