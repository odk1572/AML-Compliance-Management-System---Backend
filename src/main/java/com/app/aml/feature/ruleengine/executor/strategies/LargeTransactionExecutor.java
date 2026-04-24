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
public class LargeTransactionExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "LARGE_TRANSACTION"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal threshold = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("NONE".equalsIgnoreCase(cond.getAggregationFunction())) threshold = new BigDecimal(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if (threshold == null || lookback == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Large Transaction Rule.");
        }

        String sql = """
            SELECT DISTINCT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.amount >= ? AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            threshold, lookback);
        return new HashSet<>(results);
    }
}