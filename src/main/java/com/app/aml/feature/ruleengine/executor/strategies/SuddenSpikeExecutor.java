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
public class SuddenSpikeExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "SUDDEN_SPIKE"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String shortWindow = "24 hours"; 
        String longWindow = "30 days"; 
        BigDecimal multiplier = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("MULTIPLIER".equalsIgnoreCase(cond.getAttributeName())) multiplier = new BigDecimal(cond.getThresholdValue());
        }

        if (multiplier == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Sudden Spike Rule.");
        }

        String sql = """
            WITH historical_avg AS (
                SELECT originator_account_no, (SUM(amount) / 30) as daily_avg
                FROM transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) GROUP BY originator_account_no
            ),
            recent_spike AS (
                SELECT originator_account_no, SUM(amount) as recent_amount
                FROM transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) GROUP BY originator_account_no
            )
            SELECT cp.id as customer_id FROM recent_spike r
            JOIN historical_avg h ON r.originator_account_no = h.originator_account_no
            JOIN customer_profiles cp ON r.originator_account_no = cp.account_no
            WHERE h.daily_avg > 0 AND r.recent_amount > (h.daily_avg * ?)
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            longWindow, shortWindow, multiplier);
        return new HashSet<>(results);
    }
}