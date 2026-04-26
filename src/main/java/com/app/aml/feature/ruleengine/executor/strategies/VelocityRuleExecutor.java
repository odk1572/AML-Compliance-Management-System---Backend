package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext;
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
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        int threshold = 0;
        String lookback = null;

        // --- ADDED DEBUGGING LOGS ---
        log.info("Executing Velocity Rule: {}", rule.getTypologyLabel());
        log.info("Received {} conditions to evaluate.", rule.getConditions().size());

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            // Print exactly what the database handed us
            log.info("Condition Dump -> AggFn: '{}', Threshold: '{}', Lookback: '{}'",
                    cond.getAggregationFunction(), cond.getThresholdValue(), cond.getLookbackPeriod());

            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase().trim() : "NONE";

            if ("COUNT".equals(agg)) {
                // Use trim() to prevent unseen spaces from breaking the parser
                threshold = Integer.parseInt(cond.getThresholdValue().trim());
                if (cond.getLookbackPeriod() != null && !cond.getLookbackPeriod().isBlank()) {
                    lookback = cond.getLookbackPeriod().trim();
                }
            }
        }

        if (threshold <= 0 || lookback == null) {
            log.error("CRITICAL DATA MISSING! Threshold resolved to: {}, Lookback resolved to: {}", threshold, lookback);
            throw new IllegalStateException("Required parameters missing for Velocity Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            SELECT cp.id as customer_id 
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id 
            HAVING COUNT(t.id) >= ?
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                interval,
                threshold);

        return new HashSet<>(results);
    }
}