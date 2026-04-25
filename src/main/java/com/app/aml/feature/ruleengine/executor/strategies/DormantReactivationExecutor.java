package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext; // Assumed helper
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
public class DormantReactivationExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "DORMANT_REACTIVATION"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String dormantPeriod = null;      // e.g., '180 days'
        String reactivationWindow = null; // e.g., '7 days'
        BigDecimal threshold = null;

        // 1. Robust Parameter Extraction
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("MIN".equalsIgnoreCase(cond.getAggregationFunction())) {
                dormantPeriod = cond.getLookbackPeriod();
            } else if ("MAX".equalsIgnoreCase(cond.getAggregationFunction())) {
                reactivationWindow = cond.getLookbackPeriod();
            } else {
                threshold = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (dormantPeriod == null || reactivationWindow == null || threshold == null) {
            throw new IllegalStateException("Missing conditions for rule: " + rule.getRuleName());
        }

        // 2. Schema-Aware SQL
        // We use the account_number from both sides (Originator/Beneficiary)
        // to catch all "activity"
        String schema = TenantContext.getSchemaName();
        String sql = String.format("""
            SELECT cp.id as customer_id 
            FROM %s.customer_profiles cp
            JOIN %s.transactions t 
              ON (t.originator_account_no = cp.account_number OR t.beneficiary_account_no = cp.account_number)
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id
            HAVING SUM(t.amount) >= ?
               AND NOT EXISTS (
                   SELECT 1 FROM %s.transactions t2 
                   WHERE (t2.originator_account_no = cp.account_number OR t2.beneficiary_account_no = cp.account_number)
                     AND t2.transaction_timestamp < CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                     AND t2.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) - CAST(? AS INTERVAL)
               )
        """, schema, schema, schema);

        log.debug("Executing Dormant Reactivation for tenant: {}", schema);

        // 3. Execution
        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                reactivationWindow, // Activity window
                threshold,          // Min amount
                reactivationWindow, // End of dormancy gap
                reactivationWindow, // Start of dormancy gap
                dormantPeriod       // Duration of dormancy
        );

        return new HashSet<>(results);
    }
}