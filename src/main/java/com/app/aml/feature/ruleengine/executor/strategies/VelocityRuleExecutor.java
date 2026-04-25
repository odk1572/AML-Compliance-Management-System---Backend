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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
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
        int targetCount = 0;
        String scenarioLookbackRaw = null; // Variable A
        String ruleTimeWindowRaw = null;   // Variable B

        // 1. Extract variables using pure attribute mapping
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.TRANSACTION_COUNT ->
                        targetCount = Integer.parseInt(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Mandatory Parameter & Logic Validation
        if (targetCount <= 0 || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        // Centralized check: Scenario Lookback must cover Rule Window
        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Prepare variables for SQL
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);
        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic (CTE for Context + Aggregation for Rule)
        String sql = String.format("""
            -- STEP 1: Apply Scenario Lookback (Variable A)
            WITH scenario_context AS (
                SELECT originator_account_no, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Apply Velocity Logic within Rule Time Window (Variable B)
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id 
            HAVING COUNT(*) >= ?
        """, schema, schema);

        log.debug("Executing Velocity Rule for tenant: {} (Lookback: {}, Window: {})",
                schema, scenarioLookback, ruleTimeWindow);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback, // 1st ?: CTE Boundary
                ruleTimeWindow,   // 2nd ?: Rule specific window
                targetCount       // 3rd ?: Minimum transaction count
        );

        return new HashSet<>(results);
    }
}