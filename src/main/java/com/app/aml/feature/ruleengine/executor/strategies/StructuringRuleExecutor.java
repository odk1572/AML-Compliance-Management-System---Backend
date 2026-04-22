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
public class StructuringRuleExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "STRUCTURING"; }

    @Override
    public Set<String> executeRule(RuleExecutionContextDto rule) {
//        BigDecimal singleLimit = new BigDecimal("10000");
//        BigDecimal totalThreshold = new BigDecimal("10000");
//        int splitCount = 2;
//        String lookback = "24 hours";
        BigDecimal singleLimit = null;
        BigDecimal totalThreshold = null;
        int splitCount = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("NONE".equalsIgnoreCase(cond.getAggregationFunction())) singleLimit = new BigDecimal(cond.getThresholdValue());
            if ("SUM".equalsIgnoreCase(cond.getAggregationFunction())) totalThreshold = new BigDecimal(cond.getThresholdValue());
            if ("COUNT".equalsIgnoreCase(cond.getAggregationFunction())) splitCount = Integer.parseInt(cond.getThresholdValue());
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if(singleLimit == null || totalThreshold == null || splitCount == 0 || lookback == null){
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Smurfing Rule.");
        };

        String sql = """
            SELECT originator_account_no FROM transactions
            WHERE amount < ? AND transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY originator_account_no HAVING SUM(amount) >= ? AND COUNT(id) >= ?
        """;

        List<String> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("originator_account_no"),
                singleLimit,
                lookback,
                totalThreshold,
                splitCount);

        return new HashSet<>(results);
    }
}
