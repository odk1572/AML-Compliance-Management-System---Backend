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
public class DormantReactivationExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "DORMANT_REACTIVATION";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String dormantPeriodRaw = null;
        String reactivationWindowRaw = null;
        BigDecimal thresholdAmount = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String aggFn = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            switch (aggFn) {
                case "MIN" -> dormantPeriodRaw = cond.getLookbackPeriod();
                case "MAX" -> reactivationWindowRaw = cond.getLookbackPeriod();
                case "NONE" -> thresholdAmount = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (dormantPeriodRaw == null || reactivationWindowRaw == null || thresholdAmount == null) {
            throw new IllegalStateException("Required parameters missing for Dormant Reactivation rule: " + rule.getTypologyLabel());
        }

        String dormantPeriod = SqlIntervalParser.parse(dormantPeriodRaw);
        String reactivationWindow = SqlIntervalParser.parse(reactivationWindowRaw);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            WITH scenario_context AS (
                SELECT originator_account_no, beneficiary_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) - CAST(? AS INTERVAL)
            )
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp 
              ON (t.originator_account_no = cp.account_number OR t.beneficiary_account_no = cp.account_number)
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id
            HAVING SUM(t.amount) >= ?
               AND NOT EXISTS (
                   SELECT 1 FROM scenario_context t2 
                   WHERE (t2.originator_account_no = cp.account_number OR t2.beneficiary_account_no = cp.account_number)
                     AND t2.transaction_timestamp < CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                     AND t2.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) - CAST(? AS INTERVAL)
               )
        """, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                reactivationWindow,
                dormantPeriod,
                reactivationWindow,
                thresholdAmount,
                reactivationWindow,
                reactivationWindow,
                dormantPeriod);

        return new HashSet<>(results);
    }
}