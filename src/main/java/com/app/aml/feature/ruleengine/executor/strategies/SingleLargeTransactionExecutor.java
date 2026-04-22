package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SingleLargeTransactionExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "LARGE_TRANSACTION"; }

    @Override
    public Set<String> executeRule(RuleExecutionContextDto rule) {
//        BigDecimal threshold = new BigDecimal("50000");
//        String lookback = "24 hours";
        BigDecimal threshold = null;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("NONE".equalsIgnoreCase(cond.getAggregationFunction())) threshold = new BigDecimal(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if(threshold == null || lookback == null){
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Smurfing Rule.");
        }

        String sql = """
            SELECT originator_account_no FROM transactions
            WHERE amount >= ? AND transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
        """;

        List<String> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("originator_account_no"),
                threshold,
                lookback);

        return new HashSet<>(results);
    }
}
