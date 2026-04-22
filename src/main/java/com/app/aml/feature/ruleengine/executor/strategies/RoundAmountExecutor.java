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
public class RoundAmountExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "ROUND_AMOUNT"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        int divisor = 0;
        int threshold = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("DIVISOR".equalsIgnoreCase(cond.getAttributeName())) divisor = Integer.parseInt(cond.getThresholdValue());
            if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) threshold = Integer.parseInt(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if (divisor == 0 || threshold == 0 || lookback == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Round Amount Rule.");
        }

        String sql = """
            SELECT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.originator_account_no = cp.account_no
            WHERE MOD(t.amount, ?) = 0 AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id HAVING COUNT(t.id) >= ?
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            divisor, lookback, threshold);
        return new HashSet<>(results);
    }
}