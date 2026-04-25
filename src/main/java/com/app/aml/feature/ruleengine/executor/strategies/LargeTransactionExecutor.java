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
public class LargeTransactionExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "LARGE_TRANSACTION";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal thresholdAmount = null;
        String scenarioLookbackRaw = null; // Variable A (e.g., "30d")
        String ruleTimeWindowRaw = null;   // Variable B (e.g., "7d")

        // 1. Extract variables using the pure attribute mapping
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.TRANSACTION_AMOUNT ->
                        thresholdAmount = new BigDecimal(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Mandatory Parameter & Logic Validation
        if (thresholdAmount == null || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        // Use helper to ensure Variable A covers Variable B
        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Parse for SQL use
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);

        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic (CTE for Context + Filtering for Rule)
        String sql = String.format("""
            -- STEP 1: Apply Variable A (Scenario Lookback) to get transactions into consideration
            WITH scenario_context AS (
                SELECT originator_account_no, beneficiary_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Filter the considered transactions by Variable B (Rule Time Window) and Thresholds
            SELECT DISTINCT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp 
              ON (t.originator_account_no = cp.account_number OR t.beneficiary_account_no = cp.account_number)
            WHERE t.amount >= ? 
              AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
        """, schema, schema);

        log.debug("Executing Large Transaction Rule for tenant: {} (Lookback: {}, TimeWindow: {})",
                schema, scenarioLookback, ruleTimeWindow);

        // 5. Inject variables in the exact order of the ? placeholders
        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback, // 1st ?: CTE Boundary
                thresholdAmount,  // 2nd ?: Amount check
                ruleTimeWindow    // 3rd ?: Specific rule filter
        );

        return new HashSet<>(results);
    }
}