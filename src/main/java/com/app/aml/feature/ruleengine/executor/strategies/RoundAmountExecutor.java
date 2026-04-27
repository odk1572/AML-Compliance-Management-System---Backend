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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Component
public class RoundAmountExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    @Autowired
    public RoundAmountExecutor(JdbcTemplate jdbcTemplate) {
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
        return "ROUND_AMOUNT";
    }

    @Override
    public List<RuleBreachResult> executeRule(RuleExecutionContextDto rule) {
        int divisor = 0;
        int targetCount = 0;
        String lookback = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";

            if (cond.getLookbackPeriod() != null) {
                lookback = cond.getLookbackPeriod();
            }

            switch (agg) {
                case "NONE" -> divisor = Integer.parseInt(cond.getThresholdValue());
                case "COUNT" -> targetCount = Integer.parseInt(cond.getThresholdValue());
            }
        }

        if (divisor <= 0 || targetCount <= 0 || lookback == null) {
            throw new IllegalStateException("Required parameters missing for Round Amount Rule: " + rule.getTypologyLabel());
        }

        String interval = SqlIntervalParser.parse(lookback);
        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            SELECT 
                row_to_json(cp.*) as customer_json, 
                json_agg(row_to_json(t.*)) as transactions_json
            FROM %s.transactions t
            JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
            WHERE t.transaction_timestamp >= CURRENT_TIMESTAMP - CAST(? AS INTERVAL)
              AND MOD(t.amount, ?) = 0 
            GROUP BY cp.id 
            HAVING COUNT(t.id) >= ?
        """, schema, schema);

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
        }, interval, divisor, targetCount);
    }
}