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
        String scenarioLookbackRaw = null;
        String ruleTimeWindowRaw = null;

        // 1. Extract variables using your specific Typology Attributes
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.SINGLE_TRANSACTION_LIMIT ->
                        singleLimit = new BigDecimal(cond.getThresholdValue());
                case RuleAttributeConstants.TOTAL_STRUCTURED_AMOUNT ->
                        totalThreshold = new BigDecimal(cond.getThresholdValue());
                case RuleAttributeConstants.SPLIT_TRANSACTION_COUNT ->
                        splitCount = Integer.parseInt(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Validation
        if (singleLimit == null || totalThreshold == null || splitCount <= 0
                || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Prepare SQL Values
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);
        String schema = TenantContext.getSchemaName();

        // 4. The 2-Step SQL Logic
        String sql = String.format("""
            -- STEP 1: Scenario boundary
            WITH scenario_context AS (
                SELECT originator_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            -- STEP 2: Rule logic for 'Splits'
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND t.amount < ? 
            GROUP BY cp.id 
            HAVING SUM(t.amount) >= ? 
               AND COUNT(*) >= ?
        """, schema, schema);

        log.debug("Executing Structuring for tenant: {} (Total: >= {}, Limit: < {})",
                schema, totalThreshold, singleLimit);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback,
                ruleTimeWindow,
                singleLimit,
                totalThreshold,
                splitCount
        );

        return new HashSet<>(results);
    }
}