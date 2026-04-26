package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext;
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

    @Override
    public String getRuleType() {
        return "ROUND_AMOUNT";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        int divisor = 0;
        int targetCount = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            if (cond.getLookbackPeriod() != null) {
                lookback = cond.getLookbackPeriod();
            }

            switch (agg) {
                case "NONE" -> divisor = Integer.parseInt(cond.getThresholdValue());
                case "COUNT" -> targetCount = Integer.parseInt(cond.getThresholdValue());
            }
        }

        if (divisor <= 0 || targetCount <= 0 || lookback == null) {
            throw new IllegalStateException("Required parameters missing for Round Amount Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            SELECT cp.id as customer_id 
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND MOD(t.amount, ?) = 0 
            GROUP BY cp.id 
            HAVING COUNT(t.id) >= ?
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                interval,
                divisor,
                targetCount);

        return new HashSet<>(results);
    }
}