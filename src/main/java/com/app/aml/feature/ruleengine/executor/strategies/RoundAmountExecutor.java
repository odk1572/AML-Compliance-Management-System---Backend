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
public class RoundAmountExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "ROUND_AMOUNT";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        int divisor = 0;
        int targetCount = 0;
        String scenarioLookbackRaw = null;
        String ruleTimeWindowRaw = null;

        // 1. Extract variables using existing constants
        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;

            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.TRANSACTION_AMOUNT ->
                        divisor = Integer.parseInt(cond.getThresholdValue());
                case RuleAttributeConstants.TRANSACTION_COUNT ->
                        targetCount = Integer.parseInt(cond.getThresholdValue());
                case RuleAttributeConstants.LOOKBACK_WINDOW ->
                        scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW ->
                        ruleTimeWindowRaw = cond.getThresholdValue();
            }
        }

        // 2. Validation
        if (divisor <= 0 || targetCount <= 0 || scenarioLookbackRaw == null || ruleTimeWindowRaw == null) {
            throw new IllegalStateException(String.format(
                    "Missing required conditions for rule: %s.", rule.getRuleName()));
        }

        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 3. Prepare SQL values
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);
        String schema = TenantContext.getSchemaName();

        // 4. Dual-Timeframe SQL Logic
        String sql = String.format("""
            WITH scenario_context AS (
                SELECT originator_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            )
            SELECT cp.id as customer_id 
            FROM scenario_context t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND MOD(t.amount, ?) = 0 
            GROUP BY cp.id 
            HAVING COUNT(*) >= ?
        """, schema, schema);

        log.debug("Executing Round Amount Rule for tenant: {}", schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback,
                ruleTimeWindow,
                divisor,
                targetCount
        );

        return new HashSet<>(results);
    }
}