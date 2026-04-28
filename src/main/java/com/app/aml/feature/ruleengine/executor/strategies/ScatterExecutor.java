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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Component
public class ScatterExecutor implements RuleExecutorStrategy {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ScatterExecutor(JdbcTemplate jdbcTemplate) {
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
        return "SCATTER";
    }

    @Override
    public List<RuleBreachResult> executeRule(RuleExecutionContextDto rule) {
        int targetCount = 0;
        String chunkSize = null;

        for (ConditionExecutionContextDto cond : rule.getConditions()) {
            String agg = cond.getAggregationFunction() != null ? cond.getAggregationFunction().toUpperCase() : "NONE";
            if ("COUNT".equals(agg)) {
                targetCount = Integer.parseInt(cond.getThresholdValue());
                if (cond.getLookbackPeriod() != null) {
                    chunkSize = cond.getLookbackPeriod();
                }
            }
        }

        if (targetCount <= 0 || chunkSize == null) {
            throw new IllegalStateException("Required parameters missing for Scatter Rule: " + rule.getTypologyLabel());
        }

        if (rule.getGlobalLookbackStart() == null || rule.getGlobalLookbackEnd() == null || rule.getDataFetchStart() == null) {
            throw new IllegalStateException("Global Start, End, and Data Fetch Start (Buffer) times are required for execution.");
        }

        String schema = TenantContext.getSchemaName();

        String sql = String.format("""
            WITH data_universe AS (
                SELECT t.*, cp.id as customer_profile_id, cp.account_number
                FROM %s.transactions t
                JOIN %s.customer_profiles cp ON t.originator_account_no = cp.account_number
                WHERE t.transaction_timestamp BETWEEN ? AND ?
            ),
            breaching_customers AS (
                SELECT DISTINCT t1.customer_profile_id
                FROM data_universe t1
                WHERE t1.transaction_timestamp BETWEEN ? AND ?
                AND (
                    SELECT COUNT(DISTINCT t2.beneficiary_account_no)
                    FROM data_universe t2
                    WHERE t2.originator_account_no = t1.account_number
                      AND t2.transaction_timestamp BETWEEN t1.transaction_timestamp - CAST(? AS INTERVAL) AND t1.transaction_timestamp
                ) >= ?
            )
            SELECT 
                ? as rule_type,
                ? as rule_label,
                row_to_json(cp.*) as customer_json, 
                json_agg(row_to_json(du.*)) as transactions_json
            FROM data_universe du
            JOIN breaching_customers bc ON du.customer_profile_id = bc.customer_profile_id
            JOIN %s.customer_profiles cp ON bc.customer_profile_id = cp.id
            WHERE du.transaction_timestamp BETWEEN ? AND ?
            GROUP BY cp.id, rule_type, rule_label
        """, schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
                    try {
                        CustomerProfile customer = objectMapper.readValue(
                                rs.getString("customer_json"), CustomerProfile.class
                        );
                        List<Transaction> transactions = objectMapper.readValue(
                                rs.getString("transactions_json"), new TypeReference<List<Transaction>>() {}
                        );

                        String type = rs.getString("rule_type");
                        String label = rs.getString("rule_label");

                        return new RuleBreachResult(customer, transactions, type, label);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON from database", e);
                    }
                },
                Timestamp.from(rule.getDataFetchStart()),
                Timestamp.from(rule.getGlobalLookbackEnd()),
                Timestamp.from(rule.getGlobalLookbackStart()),
                Timestamp.from(rule.getGlobalLookbackEnd()),
                chunkSize,
                targetCount,
                getRuleType(),
                rule.getTypologyLabel(),
                Timestamp.from(rule.getGlobalLookbackStart()),
                Timestamp.from(rule.getGlobalLookbackEnd()));
    }
}