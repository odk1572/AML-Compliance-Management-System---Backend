package com.app.aml.feature.ruleengine.executor.strategies;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ruleengine.dto.RuleBreachResult;
import com.app.aml.feature.ruleengine.dto.execution.ConditionExecutionContextDto;

import com.app.aml.feature.ruleengine.dto.execution.RuleExecutionContextDto;
import com.app.aml.feature.ruleengine.executor.RuleExecutorStrategy;
import com.app.aml.multitenency.TenantContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Component
public class SuddenSpikeExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    @Autowired
    public SuddenSpikeExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        DateTimeFormatter pgFormatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .optionalEnd()
                .toFormatter();
        SimpleModule instantModule = new SimpleModule();
        instantModule.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText().trim().replace(" ", "T");

                TemporalAccessor accessor = pgFormatter.parseBest(text, Instant::from, LocalDateTime::from);

                if (accessor instanceof Instant instant) {
                    return instant;
                }
                return ((LocalDateTime) accessor).toInstant(ZoneOffset.UTC);
            }
        });

        mapper.registerModule(instantModule);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.objectMapper = mapper;
    }


    @Override
    public String getRuleType() {
        return "SUDDEN_SPIKE";
    }

    @Override
    public List<RuleBreachResult> executeRule(RuleExecutionContextDto rule) {
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
            ),
            violating_accounts AS (
                SELECT r.originator_account_no
                FROM recent_activity r
                JOIN historical_baseline h ON r.originator_account_no = h.originator_account_no
                WHERE h.daily_avg > 0
                  AND r.recent_sum > (h.daily_avg * ?)
            )
            SELECT 
                row_to_json(cp.*) as customer_json, 
                json_agg(row_to_json(t.*)) as transactions_json
            FROM violating_accounts va
            JOIN %s.customer_profiles cp ON va.originator_account_no = cp.account_number
            JOIN %s.transactions t ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
            GROUP BY cp.id
        """, schema, schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                CustomerProfile customer = objectMapper.readValue(
                        rs.getString("customer_json"),
                        CustomerProfile.class
                );

                List<Transaction> transactions = objectMapper.readValue(
                        rs.getString("transactions_json"),
                        new TypeReference<List<Transaction>>() {}
                );

                return new RuleBreachResult(customer, transactions);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON from database", e);
            }
        }, lookbackDays, longInterval, shortInterval, multiplier, shortInterval);
    }
}