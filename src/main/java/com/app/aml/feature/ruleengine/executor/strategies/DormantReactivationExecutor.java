package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.domain.constants.RuleAttributeConstants;
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
public class DormantReactivationExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "DORMANT_REACTIVATION";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        // Raw strings for validation
        String scenarioLookbackRaw = null;
        String dormantPeriodRaw = null;
        String reactivationWindowRaw = null;
        BigDecimal thresholdAmount = null;

        // 1. Extract variables using the pure attribute mapping
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.DORMANT_PERIOD ->
                        dormantPeriodRaw = cond.getThresholdValue();
                case RuleAttributeConstants.REACTIVATION_WINDOW ->
                        reactivationWindowRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TRANSACTION_AMOUNT ->
                        thresholdAmount = new BigDecimal(cond.getThresholdValue());
            }
        }

        // 2. Perform Mandatory Checks and Timeframe Validation
        if (scenarioLookbackRaw == null || dormantPeriodRaw == null || reactivationWindowRaw == null || thresholdAmount == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        // Use the centralized helper to ensure Lookback >= Reactivation + Dormancy
        SqlIntervalParser.validateCoverage(
                scenarioLookbackRaw,
                rule.getRuleName(),
                reactivationWindowRaw, dormantPeriodRaw
        );

        // 3. Prepare Parsed Intervals for SQL
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String reactivationWindow = SqlIntervalParser.parse(reactivationWindowRaw);
        String dormantPeriod = SqlIntervalParser.parse(dormantPeriodRaw);

        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic (CTE + Filter)
        String sql = String.format("""
            -- STEP 1: Apply Variable A (Scenario Lookback Context)
            WITH scenario_context AS (
                SELECT originator_account_no, beneficiary_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Filter by Variable B (Reactivation) and confirm historical Dormancy
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

        log.debug("Executing Dormant Reactivation for tenant: {} (Lookback: {})", schema, scenarioLookback);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback,   // CTE Boundary
                reactivationWindow, // WHERE Filter
                thresholdAmount,    // Aggregation Check
                reactivationWindow, // NOT EXISTS Window Start
                reactivationWindow, // NOT EXISTS Window End (part 1)
                dormantPeriod       // NOT EXISTS Window End (part 2)
        );

        return new HashSet<>(results);
    }
}