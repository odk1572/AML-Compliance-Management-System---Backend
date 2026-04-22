package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;
import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DormantReactivationExecutor implements RuleExecutorStrategy {
    private final JdbcTemplate jdbcTemplate;

    @Override public String getRuleType() { return "DORMANT_REACTIVATION"; }

    @Override
    public Set<UUID> executeRule(RuleExecutionContextDto rule) {
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if (cond.getLookbackPeriod() != null) lookback = SqlIntervalParser.parse(cond.getLookbackPeriod());
        }

        if (lookback == null) {
            throw new IllegalStateException("Missing required global or tenant condition thresholds for Dormant Reactivation Rule.");
        }

        String sql = """
            SELECT DISTINCT cp.id as customer_id FROM transactions t
            JOIN customer_profiles cp ON t.originator_account_no = cp.account_no
            WHERE cp.status = 'DORMANT' AND t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
        """;
        
        List<UUID> results = jdbcTemplate.query(sql, 
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")), 
            lookback);
        return new HashSet<>(results);
    }
}