package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
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

    @Override public String getRuleType() { return "LOW_INCOME_HIGH_TRANSFER"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        BigDecimal multiplier = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            // MULTIPLIER is a domain-specific concept (not a standard aggregation function),
            // so it is matched by attributeName rather than aggregationFunction.
            if ("MULTIPLIER".equalsIgnoreCase(cond.getAttributeName())) multiplier = new BigDecimal(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if (multiplier == null || lookback == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Low Income High Transfer Rule.");
        }

        String sql = """
            SELECT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.originator_account_no = cp.account_no
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND cp.declared_annual_income IS NOT NULL
              AND cp.declared_annual_income > 0
            GROUP BY cp.id, cp.declared_annual_income
            HAVING SUM(t.amount) > (cp.declared_annual_income * ?)
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            lookback, multiplier);
        return new HashSet<>(results);
    }
}