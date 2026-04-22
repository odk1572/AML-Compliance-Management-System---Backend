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
        String dormantPeriod = null;
        String reactivationWindow = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            if ("DORMANT_PERIOD".equalsIgnoreCase(cond.getAttributeName())) {
                dormantPeriod = SqlIntervalParser.parse(cond.getLookbackPeriod());
            }
            if ("REACTIVATION_WINDOW".equalsIgnoreCase(cond.getAttributeName())) {
                reactivationWindow = SqlIntervalParser.parse(cond.getLookbackPeriod());
            }
        }

        if (dormantPeriod == null || reactivationWindow == null) {
            throw new IllegalStateException("Missing required conditions for Dormant Reactivation Rule. "
                    + "Need: DORMANT_PERIOD (attributeName + lookbackPeriod), REACTIVATION_WINDOW (attributeName + lookbackPeriod).");
        }

        // Find customers who:
        // 1. Have at least one transaction in the reactivation window (recent activity)
        // 2. Had NO transactions during the dormant period before the reactivation window
        String sql = """
            SELECT DISTINCT cp.id as customer_id FROM transactions t_recent
            JOIN customer_profiles cp ON t_recent.originator_account_no = cp.account_no
            WHERE t_recent.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND NOT EXISTS (
                  SELECT 1 FROM transactions t_dormant
                  WHERE t_dormant.originator_account_no = t_recent.originator_account_no
                    AND t_dormant.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
                    AND t_dormant.transaction_timestamp < CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              )
        """;

        List<UUID> results = jdbcTemplate.query(sql,
            (rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
            reactivationWindow, dormantPeriod, reactivationWindow);
        return new HashSet<>(results);
    }
}