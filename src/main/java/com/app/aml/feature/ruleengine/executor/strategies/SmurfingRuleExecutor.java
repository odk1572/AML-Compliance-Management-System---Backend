package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.ConditionExecutionDto;
import com.app.aml.feature.ruleengine.dto.RuleExecutionDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmurfingRuleExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "SMURFING";
    }

    @Override
    public Set<String> executeRule(RuleExecutionDto rule, UUID batchId) {
        log.info("Executing Smurfing Rule [{}] for Batch [{}]", rule.getRuleCode(), batchId);

        // Initialize variables
        BigDecimal singleTxLimit = null; 
        BigDecimal aggregateThreshold = null; 
        int splitCount = 0; 
        String lookbackInterval = null; 

        // Extract dynamic DB configurations (Global + Tenant overrides)
        if (rule.getConditions() != null) {
            for (ConditionExecutionDto cond : rule.getConditions()) {
                if ("NONE".equalsIgnoreCase(cond.getAggregationFunction()) && "amount".equalsIgnoreCase(cond.getAttributeName())) {
                    singleTxLimit = new BigDecimal(cond.getThresholdValue());
                }
                else if ("SUM".equalsIgnoreCase(cond.getAggregationFunction())) {
                    aggregateThreshold = new BigDecimal(cond.getThresholdValue());
                }
                else if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) {
                    splitCount = Integer.parseInt(cond.getThresholdValue());
                }
                
                if (cond.getLookbackPeriod() != null) {
                    lookbackInterval = cond.getLookbackPeriod().replace("h", " hours").replace("d", " days");
                }
            }
        }

        if (singleTxLimit == null || aggregateThreshold == null || splitCount == 0 || lookbackInterval == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Smurfing Rule.");
        }

        String sql = """
            SELECT originator_account_no
            FROM transactions
            WHERE batch_id = ?
              AND amount < ? 
              AND transaction_timestamp >= (
                  SELECT MAX(transaction_timestamp) 
                  FROM transactions 
                  WHERE batch_id = ?
              ) - CAST(? as INTERVAL)
            GROUP BY originator_account_no
            HAVING SUM(amount) >= ?  
               AND COUNT(id) >= ?    
        """;

        List<String> results = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getString("originator_account_no"),
            batchId,             
            singleTxLimit,       
            batchId,             
            lookbackInterval,    
            aggregateThreshold,  
            splitCount           
        );

        return new HashSet<>(results);
    }
}
