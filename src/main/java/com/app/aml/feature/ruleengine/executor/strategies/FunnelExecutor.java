package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FunnelExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "FUNNEL"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        int targetCount = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) targetCount = Integer.parseInt(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if (targetCount == 0 || lookback == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Funnel Rule.");
        }

        String sql = """
            SELECT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.beneficiary_account_no = cp.account_no
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id HAVING COUNT(DISTINCT t.originator_account_no) >= ?
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            lookback, targetCount);
        return new HashSet<>(results);
    }
}