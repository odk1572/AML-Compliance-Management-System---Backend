package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LargeTransactionExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "LARGE_TRANSACTION";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal threshold = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            if ("NONE".equals(agg)) {
                threshold = new BigDecimal(cond.getThresholdValue());
                if (cond.getLookbackPeriod() != null) {
                    lookback = cond.getLookbackPeriod();
                }
            }
        }

        if (threshold == null || lookback == null) {
            throw new IllegalStateException("Required parameters missing for Large Transaction Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            SELECT DISTINCT cp.id as customer_id 
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON (t.originator_account_no = cp.account_number OR t.beneficiary_account_no = cp.account_number)
            WHERE t.amount >= ? 
              AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                threshold,
                interval);

        return new HashSet<>(results);
    }
}