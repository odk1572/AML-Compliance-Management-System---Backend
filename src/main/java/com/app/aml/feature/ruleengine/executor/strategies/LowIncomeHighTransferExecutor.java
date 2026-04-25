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
public class LowIncomeHighTransferExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "LOW_INCOME_HIGH_TRANSFER";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal multiplier = null;
        String scenarioLookbackRaw = null; // Variable A
        String ruleTimeWindowRaw = null;   // Variable B

        // 1. Extract variables using pure attribute mapping
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.MULTIPLIER ->
                        multiplier = new BigDecimal(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Mandatory Parameter & Logic Validation
        if (multiplier == null || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        // Use helper to ensure Scenario Context >= Rule Window
        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Parse for SQL use
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);

        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic (CTE for Context + Income Calculation for Rule)
        String sql = String.format("""
            -- STEP 1: Bound initial transactions by Variable A (Scenario Lookback)
            WITH scenario_context AS (
                SELECT originator_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Aggregate by Variable B (Rule Time Window) and check against Income Multiplier
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND cp.monthly_income IS NOT NULL
              AND cp.monthly_income > 0
            GROUP BY cp.id, cp.monthly_income
            HAVING SUM(t.amount) > (cp.monthly_income * ?)
        """, schema, schema);

        log.debug("Executing Low Income High Transfer Rule for tenant: {} (Lookback: {}, Window: {})",
                schema, scenarioLookback, ruleTimeWindow);

        // 5. Inject variables in order
        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback, // 1st ?: CTE Boundary
                ruleTimeWindow,   // 2nd ?: WHERE filter
                multiplier        // 3rd ?: HAVING multiplier
        );

        return new HashSet<>(results);
    }
}