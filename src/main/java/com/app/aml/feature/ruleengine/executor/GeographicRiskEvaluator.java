package com.app.aml.feature.ruleengine.executor;

import com.app.aml.multitenency.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeographicRiskEvaluator {

    private final JdbcTemplate jdbcTemplate;

    public double getGeographicRiskMultiplier(UUID customerId) {
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            WITH involved_countries AS (
                -- 1. Customer's Residency
                SELECT country_of_residence AS country_code 
                FROM %s.customer_profiles 
                WHERE id = ?
                
                UNION
                
                -- 2. Destination of OUTGOING funds (Customer sent money)
                SELECT t.beneficiary_country AS country_code 
                FROM %s.transactions t
                JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
                WHERE cp.id = ?
                  AND t.transaction_timestamp >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                  
                UNION
                
                -- 3. Origin of INCOMING funds (Customer received money)
                SELECT t.originator_country AS country_code 
                FROM %s.transactions t
                JOIN %s.customer_profiles cp ON t.beneficiary_account_no = cp.account_number
                WHERE cp.id = ?
                  AND t.transaction_timestamp >= CURRENT_TIMESTAMP - INTERVAL '30 days'
            )
            SELECT gr.risk_tier, gr.basel_aml_index_score 
            FROM involved_countries ic
            JOIN common_schema.geographic_risk_ratings gr ON ic.country_code = gr.country_code
            WHERE gr.sys_is_deleted = false
            ORDER BY 
               CASE gr.risk_tier 
                   WHEN 'CRITICAL' THEN 1 
                   WHEN 'HIGH' THEN 2 
                   WHEN 'MEDIUM' THEN 3 
                   ELSE 4 
               END,
               gr.basel_aml_index_score DESC
            LIMIT 1
        """, schema, schema, schema, schema, schema);

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String riskTier = rs.getString("risk_tier").toUpperCase();
                int baselScore = rs.getInt("basel_aml_index_score");

                double multiplier = switch (riskTier) {
                    case "CRITICAL" -> 2.0;
                    case "HIGH"     -> (baselScore > 70) ? 1.75 : 1.5;
                    case "MEDIUM"   -> 1.2;
                    default         -> 1.0;
                };

                log.debug("Geo-Risk for Customer {}: Tier={}, BaselScore={}, Multiplier={}",
                        customerId, riskTier, baselScore, multiplier);

                return multiplier;
            }, customerId, customerId, customerId);

        } catch (EmptyResultDataAccessException e) {
            log.trace("No geographic risk modifiers found for Customer {}. Defaulting to 1.0x", customerId);
            return 1.0;
        } catch (DataAccessException e) {
            log.error("Database error while evaluating geographic risk for Customer {}. Defaulting to 1.0x", customerId, e);
            return 1.0;
        }
    }
}