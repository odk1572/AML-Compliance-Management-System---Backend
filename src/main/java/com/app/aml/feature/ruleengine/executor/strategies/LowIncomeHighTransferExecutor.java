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
public class LowIncomeHighTransferExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "LOW_INCOME_HIGH_TRANSFER";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal multiplier = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            if (cond.getLookbackPeriod() != null) {
                lookback = cond.getLookbackPeriod();
            }

            if ("NONE".equals(agg)) {
                multiplier = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (multiplier == null || lookback == null) {
            throw new IllegalStateException("Required parameters missing for Low Income High Transfer Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            SELECT cp.id as customer_id 
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND cp.monthly_income IS NOT NULL
              AND cp.monthly_income > 0
            GROUP BY cp.id, cp.monthly_income
            HAVING SUM(t.amount) > (cp.monthly_income * ?)
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                interval,
                multiplier);

        return new HashSet<>(results);
    }
}