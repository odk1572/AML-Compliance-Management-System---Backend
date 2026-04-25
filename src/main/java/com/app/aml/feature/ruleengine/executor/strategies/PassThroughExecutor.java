package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext;
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
public class PassThroughExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "PASS_THROUGH";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String lookback = null;
        BigDecimal margin = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("NONE".equalsIgnoreCase(cond.getAggregationFunction()) || cond.getAggregationFunction() == null) {
                margin = new BigDecimal(cond.getThresholdValue());
            }
            if (cond.getLookbackPeriod() != null) {
                lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
            }
        }

        if (margin == null || lookback == null) {
            throw new IllegalStateException("Missing required condition thresholds for Pass Through Rule.");
        }

        String schema = TenantContext.getSchemaName();
        String sql = String.format("""
            SELECT cp.id as customer_id FROM (
                SELECT beneficiary_account_no as account_no, amount as incoming, 0 as outgoing
                FROM %s.transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                UNION ALL
                SELECT originator_account_no as account_no, 0 as incoming, amount as outgoing
                FROM %s.transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            ) flow_data
            JOIN %s.customer_profiles cp ON flow_data.account_no = cp.account_number
            GROUP BY cp.id
            HAVING SUM(incoming) > 0 
               AND SUM(outgoing) > 0 
               AND ABS(SUM(incoming) - SUM(outgoing)) <= (SUM(incoming) * ?)
        """, schema, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                lookback, lookback, margin);

        return new HashSet<>(results);
    }
}