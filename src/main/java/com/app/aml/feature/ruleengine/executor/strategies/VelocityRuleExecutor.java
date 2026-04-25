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

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) {
                threshold = Integer.parseInt(cond.getThresholdValue());
            }
            if (cond.getLookbackPeriod() != null) {
                lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
            }
        }

        if (threshold <= 0 || lookback == null) {
            throw new IllegalStateException("Required thresholds missing for Velocity Rule execution");
        }

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
                lookback, threshold);

        return new HashSet<>(results);
    }
}