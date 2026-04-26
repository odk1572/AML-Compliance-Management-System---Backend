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

        return matcher.group(1) + " " + UNIT_MAP.get(matcher.group(2));
    }

    public static void validateCoverage(String primary, String ruleName, String... subIntervals) {
        double primaryDays = convertToDays(primary);
        double totalSubDays = 0;

        for (String sub : subIntervals) {
            if (sub != null) {
                totalSubDays += convertToDays(sub);
            }
        }

        if (primaryDays < totalSubDays) {
            throw new IllegalArgumentException(String.format(
                    "Rule [%s] Validation Failed: The Scenario LOOKBACK_WINDOW (%s) is too short. " +
                            "It must cover the sum of rule timeframes provided (%s days required vs ~%.1f days provided).",
                    ruleName, primary, String.format("%.1f", totalSubDays), primaryDays
            ));
        }
    }

    private static double convertToDays(String input) {
        if (input == null || input.isBlank()) return 0;

        Matcher m = LOOKBACK_PATTERN.matcher(input.trim().toLowerCase());
        if (!m.matches()) return 0;

        double value = Double.parseDouble(m.group(1));
        return switch (m.group(2)) {
            case "h" -> value / 24.0;
            case "d" -> value;
            case "w" -> value * 7.0;
            case "m" -> value * 30.0;
            case "y" -> value * 365.0;
            default -> 0;
        };
    }

    public static double getDays(String input) {
        return convertToDays(input);
    }
}