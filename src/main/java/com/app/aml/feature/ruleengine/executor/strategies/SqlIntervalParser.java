package com.app.aml.feature.ruleengine.executor.strategies;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlIntervalParser {

    private static final Pattern LOOKBACK_PATTERN = Pattern.compile("^(\\d+)\\s*(h|d|w|m|y)$");

    private static final Map<String, String> UNIT_MAP = Map.of(
            "h", "hours",
            "d", "days",
            "w", "weeks",
            "m", "months",
            "y", "years"
    );

    private SqlIntervalParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String parse(String dbLookback) {
        if (dbLookback == null || dbLookback.isBlank()) {
            throw new IllegalArgumentException("Lookback period cannot be null or blank");
        }

        Matcher matcher = LOOKBACK_PATTERN.matcher(dbLookback.trim().toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid lookback period format: '" + dbLookback
                    + "'. Expected format: <number><unit> (e.g., 24h, 30d, 2w, 3m, 1y).");
        }

        String value = matcher.group(1);
        String unit = UNIT_MAP.get(matcher.group(2));

        return value + " " + unit;
    }
}