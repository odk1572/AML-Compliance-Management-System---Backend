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
        BigDecimal margin = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            if (cond.getLookbackPeriod() != null) {
                lookback = cond.getLookbackPeriod();
            }

            if ("NONE".equals(agg)) {
                margin = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (margin == null || lookback == null) {
            throw new IllegalStateException("Required parameters missing for Pass Through Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            WITH flow_context AS (
                SELECT beneficiary_account_no as account_no, amount as incoming, 0 as outgoing
                FROM %s.transactions 
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                UNION ALL
                SELECT originator_account_no as account_no, 0 as incoming, amount as outgoing
                FROM %s.transactions 
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            SELECT cp.id as customer_id 
            FROM flow_context t
            JOIN %s.customer_profiles cp ON t.account_no = cp.account_number
            GROUP BY cp.id
            HAVING SUM(t.incoming) > 0 
               AND SUM(t.outgoing) > 0 
               AND ABS(SUM(t.incoming) - SUM(t.outgoing)) <= (SUM(t.incoming) * ?)
        """, schema, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                interval,
                interval,
                margin);

        return new HashSet<>(results);
    }
}