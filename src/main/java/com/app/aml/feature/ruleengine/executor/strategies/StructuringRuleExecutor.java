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
public class StructuringRuleExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "STRUCTURING";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal singleLimit = null;
        BigDecimal totalThreshold = null;
        int splitCount = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("NONE".equalsIgnoreCase(cond.getAggregationFunction()) || cond.getAggregationFunction() == null) {
                singleLimit = new BigDecimal(cond.getThresholdValue());
            }
            else if ("SUM".equalsIgnoreCase(cond.getAggregationFunction())) {
                totalThreshold = new BigDecimal(cond.getThresholdValue());
            }
            else if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) {
                splitCount = Integer.parseInt(cond.getThresholdValue());
            }

            if (cond.getLookbackPeriod() != null) {
                lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
            }
        }

        if (singleLimit == null || totalThreshold == null || splitCount <= 0 || lookback == null) {
            throw new IllegalStateException("Required thresholds missing for Structuring Rule execution");
        }

        String schema = TenantContext.getSchemaName();
        String sql = String.format("""
            SELECT cp.id as customer_id 
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.amount < ? 
              AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id 
            HAVING SUM(t.amount) >= ? 
               AND COUNT(t.id) >= ?
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                singleLimit, lookback, totalThreshold, splitCount);

        return new HashSet<>(results);
    }
}