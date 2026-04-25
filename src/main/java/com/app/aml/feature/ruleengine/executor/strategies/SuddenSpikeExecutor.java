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
public class SuddenSpikeExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "SUDDEN_SPIKE";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String scenarioLookbackRaw = null;
        String ruleTimeWindowRaw = null;
        BigDecimal multiplier = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getAttributeName() == null) continue;
            switch (cond.getAttributeName().toUpperCase()) {
                case RuleAttributeConstants.LOOKBACK_WINDOW -> scenarioLookbackRaw = cond.getThresholdValue();
                case RuleAttributeConstants.TIME_WINDOW -> ruleTimeWindowRaw = cond.getThresholdValue();
                case RuleAttributeConstants.MULTIPLIER -> multiplier = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (scenarioLookbackRaw == null || ruleTimeWindowRaw == null || multiplier == null) {
            throw new IllegalStateException("Required conditions missing for Sudden Spike.");
        }

        // 1. Validate timeframe logic
        SqlIntervalParser.validateCoverage(scenarioLookbackRaw, rule.getRuleName(), ruleTimeWindowRaw);

        // 2. Prepare variables using the Helper (Respecting Private math via Public wrapper)
        String scenarioLookback = SqlIntervalParser.parse(scenarioLookbackRaw);
        String ruleTimeWindow = SqlIntervalParser.parse(ruleTimeWindowRaw);
        double lookbackDays = Math.max(1.0, SqlIntervalParser.getDays(scenarioLookbackRaw));
        String schema = TenantContext.getSchemaName();

        // 3. 3-Step CTE SQL Logic
        String sql = String.format("""
            WITH scenario_context AS (
                SELECT originator_account_no, amount, transaction_timestamp 
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            ),
            historical_baseline AS (
                SELECT originator_account_no, (SUM(amount) / ?) as daily_avg
                FROM scenario_context
                GROUP BY originator_account_no
            ),
            recent_activity AS (
                SELECT originator_account_no, SUM(amount) as recent_sum
                FROM scenario_context
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                GROUP BY originator_account_no
            )
            SELECT cp.id as customer_id
            FROM recent_activity r
            JOIN historical_baseline h ON r.originator_account_no = h.originator_account_no
            JOIN %s.customer_profiles cp ON r.originator_account_no = cp.account_number
            WHERE h.daily_avg > 0
              AND r.recent_sum > (h.daily_avg * ?)
        """, schema, schema);

        log.debug("Executing Sudden Spike for tenant: {} (Lookback Days: {})", schema, lookbackDays);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                scenarioLookback, lookbackDays, ruleTimeWindow, multiplier);

        return new HashSet<>(results);
    }
}