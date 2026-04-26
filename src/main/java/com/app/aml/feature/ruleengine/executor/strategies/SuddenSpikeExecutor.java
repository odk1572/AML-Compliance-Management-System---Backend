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
public class SuddenSpikeExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getRuleType() {
        return "SUDDEN_SPIKE";
    }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String shortWindowLookbackRaw = null;
        String longWindowLookbackRaw = null;
        BigDecimal multiplier = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String aggFn = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            switch (aggFn) {
                case "SUM" -> shortWindowLookbackRaw = cond.getLookbackPeriod();
                case "AVG" -> longWindowLookbackRaw = cond.getLookbackPeriod();
                case "NONE" -> multiplier = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (shortWindowLookbackRaw == null || longWindowLookbackRaw == null || multiplier == null) {
            throw new IllegalStateException("Required conditions missing for Sudden Spike rule: " + rule.getTypologyLabel());
        }

        String shortInterval = SqlIntervalParser.parse(shortWindowLookbackRaw);
        String longInterval = SqlIntervalParser.parse(longWindowLookbackRaw);
        double lookbackDays = Math.max(1.0, SqlIntervalParser.getDays(longWindowLookbackRaw));
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            WITH historical_baseline AS (
                SELECT originator_account_no, (SUM(amount) / ?) as daily_avg
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                GROUP BY originator_account_no
            ),
            recent_activity AS (
                SELECT originator_account_no, SUM(amount) as recent_sum
                FROM %s.transactions
                WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                GROUP BY originator_account_no
            )
            SELECT cp.id as customer_id
            FROM recent_activity r
            JOIN historical_baseline h ON r.originator_account_no = h.originator_account_no
            JOIN %s.customer_profiles cp ON r.originator_account_no = cp.account_number
            WHERE h.daily_avg > 0
              AND r.recent_sum > (h.daily_avg * ?)
        """, schema, schema, schema);

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                lookbackDays,
                longInterval,
                shortInterval,
                multiplier);

        return new HashSet<>(results);
    }
}