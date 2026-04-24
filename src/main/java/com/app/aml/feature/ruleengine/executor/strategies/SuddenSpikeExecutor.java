package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
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

    @Override public String getRuleType() { return "SUDDEN_SPIKE"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String shortWindow = null;
        String longWindow = null;
        BigDecimal multiplier = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            // Using Aggregation Function to bypass the DB attribute_name constraint
            if ("SUM".equalsIgnoreCase(cond.getAggregationFunction())) {
                shortWindow = SqlIntervalParser.parse(cond.getLookbackPeriod());
            } else if ("AVG".equalsIgnoreCase(cond.getAggregationFunction())) {
                longWindow = SqlIntervalParser.parse(cond.getLookbackPeriod());
            } else if ("NONE".equalsIgnoreCase(cond.getAggregationFunction()) || cond.getAggregationFunction() == null) {
                multiplier = new BigDecimal(cond.getThresholdValue());
            }
        }

        if (shortWindow == null || longWindow == null || multiplier == null) {
            throw new IllegalStateException("Missing required conditions for Sudden Spike Rule. Ensure conditions with SUM (short window), AVG (long window), and NONE (multiplier) are mapped.");
        }

        // Extract numeric days from longWindow for dynamic divisor (e.g., "30 days" → 30)
        int longWindowDays = extractDaysFromInterval(longWindow);

        // FIXED: Changed cp.account_no to cp.account_number
        String sql = """
            WITH historical_avg AS (
                SELECT originator_account_no, (SUM(amount) / ?) as daily_avg
                FROM transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) GROUP BY originator_account_no
            ),
            recent_spike AS (
                SELECT originator_account_no, SUM(amount) as recent_amount
                FROM transactions WHERE transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL) GROUP BY originator_account_no
            )
            SELECT cp.id as customer_id FROM recent_spike r
            JOIN historical_avg h ON r.originator_account_no = h.originator_account_no
            JOIN customer_profiles cp ON r.originator_account_no = cp.account_number
            WHERE h.daily_avg > 0 AND r.recent_amount > (h.daily_avg * ?)
        """;

        List<UUID> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
                longWindowDays, longWindow, shortWindow, multiplier);
        return new HashSet<>(results);
    }

    private int extractDaysFromInterval(String interval) {
        String[] parts = interval.trim().split("\\s+");
        int value = Integer.parseInt(parts[0]);
        String unit = parts[1].toLowerCase();

        return switch (unit) {
            case "hours", "hour" -> Math.max(1, value / 24);
            case "days", "day" -> value;
            case "weeks", "week" -> value * 7;
            case "months", "month" -> value * 30;
            case "years", "year" -> value * 365;
            default -> throw new IllegalStateException("Cannot compute days from interval: " + interval);
        };
    }
}