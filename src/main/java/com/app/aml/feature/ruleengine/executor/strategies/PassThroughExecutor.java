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
public class PassThroughExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "PASS_THROUGH";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal margin = null;
        String scenarioLookbackRaw = null; // Variable A
        String ruleTimeWindowRaw = null;   // Variable B

        // 1. Extract variables using pure attribute mapping
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.MARGIN_PERCENTAGE ->
                        margin = new BigDecimal(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Mandatory Parameter & Logic Validation
        if (margin == null || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        // Centralized check: Scenario Lookback must cover Rule Window
        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Parse for SQL use
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);

        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic (CTE with UNION + Filter/Aggregation)
        String sql = String.format("""
            -- STEP 1: Consolidate all relevant flows into the Scenario Context
            WITH scenario_context AS (
                SELECT beneficiary_account_no as account_no, amount as incoming, 0 as outgoing, transaction_timestamp
                FROM %s.transactions 
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                UNION ALL
                SELECT originator_account_no as account_no, 0 as incoming, amount as outgoing, transaction_timestamp
                FROM %s.transactions 
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Apply Rule Window and identify Pass-Through behavior within Margin
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp ON t.account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id
            HAVING SUM(t.incoming) > 0 
               AND SUM(t.outgoing) > 0 
               AND ABS(SUM(t.incoming) - SUM(t.outgoing)) <= (SUM(t.incoming) * ?)
        """, schema, schema, schema);

        log.debug("Executing Pass Through Rule for tenant: {} (Lookback: {}, Window: {})",
                schema, scenarioLookback, ruleTimeWindow);

        // 5. Inject variables in order
        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback, // 1st ?: UNION branch 1 boundary
                scenarioLookback, // 2nd ?: UNION branch 2 boundary
                ruleTimeWindow,   // 3rd ?: Final window filter
                margin            // 4th ?: Margin check
        );

        return new HashSet<>(results);
    }
}