package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DormantReactivationExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "DORMANT_REACTIVATION"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String dormantPeriod = null;
        String reactivationWindow = null;
        BigDecimal threshold = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            // Use Aggregation Function to safely map variables past DB constraints
            if ("MIN".equalsIgnoreCase(cond.getAggregationFunction())) {
                dormantPeriod = SqlIntervalParser.parse(cond.getLookbackPeriod());
            } else if ("MAX".equalsIgnoreCase(cond.getAggregationFunction())) {
                reactivationWindow = SqlIntervalParser.parse(cond.getLookbackPeriod());
            } else if ("NONE".equalsIgnoreCase(cond.getAggregationFunction()) || cond.getAggregationFunction() == null) {
                threshold = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (dormantPeriod == null || reactivationWindow == null || threshold == null) {
            throw new IllegalStateException("Missing required conditions for Dormant Reactivation Rule. Need: MIN (dormant period), MAX (reactivation window), NONE (threshold).");
        }

        // Logic: Has transactions > threshold in the recent window, but NO transactions in the dormant period before that
        String sql = """
            SELECT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id, cp.account_number
            HAVING SUM(t.amount) >= ?
               AND NOT EXISTS (
                   SELECT 1 FROM transactions t2 
                   WHERE t2.originator_account_no = cp.account_number
                     AND t2.transaction_timestamp < CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                     AND t2.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
               )
        """;

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                reactivationWindow, threshold, reactivationWindow, dormantPeriod);

        return new HashSet<>(results);
    }
}